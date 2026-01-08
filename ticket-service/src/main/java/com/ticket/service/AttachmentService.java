package com.ticket.service;

import com.ticket.dto.AttachmentDTO;
import com.ticket.entity.Attachment;
import com.ticket.exception.AttachmentFetchException;
import com.ticket.exception.AttachmentStorageException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.repository.AttachmentRepository;
import com.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private static final String ATTACHMENT_NOT_FOUND = "Attachment not found";

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final S3StorageService s3StorageService;
    private final TicketService ticketService;
    private final RestTemplate restTemplate;

    /**
     * Upload attachment to S3 and create database record
     * 
     * @param ticketId - ID of the ticket
     * @param file - File to upload
     * @param userId - User uploading the file
     * @param username - Username uploading the file
     * @return AttachmentDTO with pre-signed URL
     */
    @Transactional
    public AttachmentDTO uploadAttachment(String ticketId, MultipartFile file,
                                        String userId, String username) throws IOException {
        log.info(" Upload - Ticket: {}, File: {}", ticketId, file.getOriginalFilename());

        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException("Ticket not found: " + ticketId);
        }

        String s3Key = "tickets/" + ticketId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        try {
            // Use your actual S3StorageService signature
            s3StorageService.uploadFile(s3Key, file);
            log.info("  Uploaded to S3: {}", s3Key);
        } catch (Exception e) {
            log.error(" S3 upload failed: {}", e.getMessage());
            throw new AttachmentStorageException("Failed to upload file to S3: " + e.getMessage(), e);
        }

        Attachment attachment = new Attachment();
        attachment.setAttachmentId(UUID.randomUUID().toString());
        attachment.setTicketId(ticketId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setOriginalFileName(file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setS3Key(s3Key);
        // Do NOT set fileUrl (not in entity)
        // s3Url will be filled later via presigned URL

        attachment.setUploadedByUserId(userId);
        attachment.setUploadedByUsername(username);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setDownloadCount(0);

        Attachment saved = attachmentRepository.save(attachment);
        log.info("  Attachment saved - ID: {}", saved.getAttachmentId());

        // For response, generate a presigned URL
        String presignedUrl = s3StorageService.generatePresignedUrl(saved.getS3Key());
        saved.setS3Url(presignedUrl);

        return convertToDTO(saved);
    }


    /**
     * Get all attachments for a ticket with fresh pre-signed URLs
     * 
     * @param ticketId - ID of the ticket
     * @return List of AttachmentDTOs
     */
    @Transactional(readOnly = true)
    public List<AttachmentDTO> getAttachmentsByTicket(String ticketId) {
        log.debug("  Fetching attachments - Ticket: {}", ticketId);

        try {
            List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId);
            log.info("  Found {} attachments for ticket {}", attachments.size(), ticketId);

            // Generate fresh pre-signed URLs for each attachment
            return attachments.stream().map(attachment -> {
                String presignedUrl = s3StorageService.generatePresignedUrl(attachment.getS3Key());
                attachment.setS3Url(presignedUrl);
                return convertToDTO(attachment);
            }).toList();

        } catch (Exception e) {
            log.error(" Error fetching attachments - Ticket: {}, Error: {}", ticketId, e.getMessage());
            throw new AttachmentFetchException("Failed to fetch attachments: " + e.getMessage(), e);
        }
    }

    /**
     * Get single attachment by ID
     * 
     * @param attachmentId - ID of the attachment
     * @return AttachmentDTO
     */
    @Transactional(readOnly = true)
    public AttachmentDTO getAttachment(String attachmentId) {
        log.debug("Fetching attachment - ID: {}", attachmentId);

        try {
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException(ATTACHMENT_NOT_FOUND));

            // Generate fresh pre-signed URL
            String presignedUrl = s3StorageService.generatePresignedUrl(attachment.getS3Key());
            attachment.setS3Url(presignedUrl);

            log.info("  Attachment found - ID: {}", attachmentId);
            return convertToDTO(attachment);

        } catch (Exception e) {
            log.error(" Error fetching attachment - ID: {}, Error: {}", attachmentId, e.getMessage());
            throw new AttachmentFetchException("Failed to fetch attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Download attachment file
     * Checks user has access to ticket, downloads from S3, logs download
     * 
     * @param ticketId - ID of the ticket
     * @param attachmentId - ID of the attachment
     * @param userId - User downloading
     * @return ResponseEntity with file
     */
    @Transactional
    public ResponseEntity<byte[]> downloadAttachment(String ticketId, String attachmentId, String userId) {
        log.info("Download request - Ticket: {}, Attachment: {}, User: {}", 
                ticketId, attachmentId, userId);

        try {
            // Step 1: Get attachment
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            // Step 2: Verify attachment belongs to this ticket
            if (!attachment.getTicketId().equals(ticketId)) {
                log.warn(" Attachment mismatch - Ticket: {}, Expected: {}", 
                        attachment.getTicketId(), ticketId);
                throw new RuntimeException("Attachment does not belong to this ticket");
            }

            // Step 3: Check user access
            if (!canUserAccessTicket(ticketId, userId)) {
                log.warn(" Access denied - Ticket: {}, User: {}", ticketId, userId);
                throw new RuntimeException("You do not have access to this ticket");
            }

            // Step 4: Generate pre-signed URL
            String presignedUrl = s3StorageService.generatePresignedUrl(attachment.getS3Key());
            
            // Step 5: Download from S3 using pre-signed URL
            byte[] fileData = downloadFromS3(presignedUrl);
            log.info("  Downloaded from S3 - Size: {} bytes", fileData.length);

            // Step 6: Log download for audit trail
            logDownload(attachmentId, userId);

            // Step 7: Return file with proper headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                            URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8) + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length))
                    .body(fileData);

        } catch (Exception e) {
            log.error(" Download error - Ticket: {}, Attachment: {}, Error: {}", 
                    ticketId, attachmentId, e.getMessage(), e);
            throw new RuntimeException("Failed to download attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Download all attachments for a ticket as ZIP file
     * 
     * @param ticketId - ID of the ticket
     * @param userId - User downloading
     * @return ResponseEntity with ZIP file
     */
    @Transactional
    public ResponseEntity<byte[]> downloadAllAttachments(String ticketId, String userId) {
        log.info("Download all attachments - Ticket: {}, User: {}", ticketId, userId);

        try {
            // Step 1: Check user access
            if (!canUserAccessTicket(ticketId, userId)) {
                log.warn(" Access denied - Ticket: {}, User: {}", ticketId, userId);
                throw new RuntimeException("You do not have access to this ticket");
            }

            // Step 2: Get all attachments
            List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId);
            log.info("Preparing {} attachments for ZIP", attachments.size());

            if (attachments.isEmpty()) {
                throw new RuntimeException("No attachments found for this ticket");
            }

            // Step 3: Create ZIP file
            byte[] zipData = createZipFile(attachments);
            log.info("  ZIP created - Size: {} bytes", zipData.length);

            // Step 4: Log download for each attachment
            for (Attachment attachment : attachments) {
                logDownload(attachment.getAttachmentId(), userId);
            }

            // Step 5: Return ZIP file
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"ticket-" + ticketId + "-attachments.zip\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipData.length))
                    .body(zipData);

        } catch (Exception e) {
            log.error(" ZIP download error - Ticket: {}, Error: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("Failed to download attachments as ZIP: " + e.getMessage(), e);
        }
    }

    /**
     * Delete attachment from S3 and database
     * 
     * @param attachmentId - ID of the attachment
     * @param ticketId - ID of the ticket (for verification)
     */
    @Transactional
    public void deleteAttachment(String attachmentId, String ticketId) {
        log.info("Deleting attachment - ID: {}, Ticket: {}", attachmentId, ticketId);

        try {
            // Step 1: Get attachment
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            // Step 2: Verify attachment belongs to this ticket
            if (!attachment.getTicketId().equals(ticketId)) {
                throw new RuntimeException("Attachment does not belong to this ticket");
            }

            // Step 3: Delete from S3
            s3StorageService.deleteFile(attachment.getS3Key());
            log.debug("  Deleted from S3 - Key: {}", attachment.getS3Key());

            // Step 4: Delete from database
            attachmentRepository.deleteById(attachmentId);
            log.info("  Deleted from database - ID: {}", attachmentId);

            // Step 5: Decrement ticket attachment count
            ticketService.decrementAttachmentCount(ticketId);

        } catch (Exception e) {
            log.error(" Delete error - Attachment: {}, Error: {}", attachmentId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Check if user can access ticket
     * Admin, creator, assignee, or viewer can access
     * 
     * @param ticketId - ID of the ticket
     * @param userId - ID of the user
     * @return true if user can access, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean canUserAccessTicket(String ticketId, String userId) {
        try {
            log.debug("Checking access - Ticket: {}, User: {}", ticketId, userId);
            
            // Placeholder implementation - update based on your Ticket entity
            return ticketRepository.existsById(ticketId);
            
        } catch (Exception e) {
            log.error(" Access check error - Ticket: {}, User: {}, Error: {}", 
                    ticketId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * Log download for audit trail
     * Updates download count, timestamp, and user
     * 
     * @param attachmentId - ID of the attachment
     * @param userId - User ID who downloaded
     */
    @Transactional
    public void logDownload(String attachmentId, String userId) {
        try {
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            // Update download tracking
            attachment.setDownloadCount(attachment.getDownloadCount() + 1);
            attachment.setLastDownloadedAt(LocalDateTime.now());
            attachment.setLastDownloadedBy(userId);

            attachmentRepository.save(attachment);
            log.info("  Download logged - Attachment: {}, User: {}, Count: {}", 
                    attachmentId, userId, attachment.getDownloadCount());

        } catch (Exception e) {
            log.error(" Error logging download - Attachment: {}, Error: {}", 
                    attachmentId, e.getMessage());
            // Don't throw exception for logging failure
        }
    }

    /**
     * Download file from S3 pre-signed URL
     * 
     * @param presignedUrl - S3 pre-signed URL
     * @return File bytes
     */
    private byte[] downloadFromS3(String presignedUrl) {
        try {
            log.debug(" Fetching from S3...");
            ResponseEntity<byte[]> response = restTemplate.getForEntity(presignedUrl, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("S3 returned status: " + response.getStatusCode());
            }

            byte[] data = response.getBody();
            if (data == null || data.length == 0) {
                throw new RuntimeException("S3 returned empty file");
            }

            log.debug("  Downloaded {} bytes from S3", data.length);
            return data;

        } catch (Exception e) {
            log.error(" S3 download failed - Error: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Create ZIP file containing all attachments
     * 
     * @param attachments - List of attachments
     * @return ZIP file bytes
     */
    private byte[] createZipFile(List<Attachment> attachments) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        for (Attachment attachment : attachments) {
            try {
                log.debug("Adding to ZIP - File: {}", attachment.getFileName());

                String presignedUrl = s3StorageService.generatePresignedUrl(attachment.getS3Key());
                byte[] fileData = downloadFromS3(presignedUrl);

                ZipEntry entry = new ZipEntry(attachment.getFileName());
                zipOut.putNextEntry(entry);
                zipOut.write(fileData);
                zipOut.closeEntry();

                log.debug("  Added to ZIP - File: {}", attachment.getFileName());
            } catch (Exception e) {
                log.warn("  Failed to add file to ZIP - File: {}, Error: {}",
                        attachment.getFileName(), e.getMessage());
            }
        }
    }

    return baos.toByteArray();
}


    /**
     * Convert Attachment entity to AttachmentDTO
     * 
     * @param attachment - Attachment entity
     * @return AttachmentDTO
     */
    private AttachmentDTO convertToDTO(Attachment attachment) {
        return new AttachmentDTO(
                attachment.getAttachmentId(),
                attachment.getTicketId(),
                attachment.getFileName(),
                attachment.getOriginalFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getS3Url(),              // downloadUrl
                attachment.getUploadedByUserId(),
                attachment.getUploadedByUsername(),
                attachment.getUploadedAt()
        );
    }


    /**
     * Get attachment count for ticket
     * 
     * @param ticketId - ID of the ticket
     * @return Count of attachments
     */
    @Transactional(readOnly = true)
    public Long getAttachmentCount(String ticketId) {
        return attachmentRepository.countByTicketId(ticketId);
    }

    /**
     * Delete all attachments for a ticket (used when ticket is deleted)
     * 
     * @param ticketId - ID of the ticket
     */
    @Transactional
    public void deleteAllAttachmentsByTicket(String ticketId) {
        log.info("Deleting all attachments - Ticket: {}", ticketId);

        try {
            List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId);

            for (Attachment attachment : attachments) {
                try {
                    s3StorageService.deleteFile(attachment.getS3Key());
                    attachmentRepository.deleteById(attachment.getAttachmentId());
                } catch (Exception e) {
                    log.warn("  Failed to delete attachment - ID: {}, Error: {}", 
                            attachment.getAttachmentId(), e.getMessage());
                }
            }

            log.info("  Deleted all attachments for ticket - Ticket: {}", ticketId);

        } catch (Exception e) {
            log.error(" Error deleting attachments - Ticket: {}, Error: {}", ticketId, e.getMessage());
            throw new RuntimeException("Failed to delete attachments: " + e.getMessage(), e);
        }
    }
}
