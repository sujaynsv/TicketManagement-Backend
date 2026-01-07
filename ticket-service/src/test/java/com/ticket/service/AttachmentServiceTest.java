package com.ticket.service;

import com.ticket.dto.AttachmentDTO;
import com.ticket.entity.Attachment;
import com.ticket.exception.AttachmentFetchException;
import com.ticket.exception.AttachmentStorageException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.repository.AttachmentRepository;
import com.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private S3StorageService s3StorageService;
    @Mock private TicketService ticketService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private AttachmentService attachmentService;

    private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private static Attachment attachment(String attachmentId, String ticketId, String fileName, String fileType, String s3Key) {
        Attachment a = new Attachment();
        a.setAttachmentId(attachmentId);
        a.setTicketId(ticketId);
        a.setFileName(fileName);
        a.setOriginalFileName(fileName);
        a.setFileType(fileType);
        a.setFileSize(3L);
        a.setS3Key(s3Key);
        a.setUploadedAt(LocalDateTime.now());
        a.setUploadedByUserId("u1");
        a.setUploadedByUsername("user");
        a.setDownloadCount(0);
        return a;
    }

    @Test
    void uploadAttachment_success_savesAndReturnsDto() throws Exception {
        String ticketId = "t1";
        MockMultipartFile mf = file("doc.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));

        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        Mockito.when(s3StorageService.uploadFile(anyString(), any(MockMultipartFile.class)))
       .thenReturn("some-key-or-url");
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("http://presigned");

        AttachmentDTO dto = attachmentService.uploadAttachment(ticketId, mf, "u1", "user1");

        assertNotNull(dto);
        assertEquals(ticketId, dto.ticketId());
        assertEquals("doc.txt", dto.fileName());
        assertEquals("text/plain", dto.fileType());
        assertEquals("http://presigned", dto.downloadUrl());

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertTrue(captor.getValue().getS3Key().startsWith("tickets/" + ticketId + "/"));
        assertTrue(captor.getValue().getS3Key().contains("doc.txt"));
        verify(s3StorageService).uploadFile(anyString(), eq(mf));
        verify(s3StorageService).generatePresignedUrl(anyString());
    }

    @Test
    void uploadAttachment_ticketNotFound_throwsTicketNotFoundException() {
        when(ticketRepository.existsById("t-missing")).thenReturn(false);

        MockMultipartFile mf = file("doc.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));

        assertThrows(TicketNotFoundException.class,
                () -> attachmentService.uploadAttachment("t-missing", mf, "u1", "user1"));

        verifyNoInteractions(s3StorageService);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadAttachment_s3UploadFails_throwsAttachmentStorageException() throws Exception {
        when(ticketRepository.existsById("t1")).thenReturn(true);

        MockMultipartFile mf = file("doc.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));
        doThrow(new RuntimeException("boom")).when(s3StorageService).uploadFile(anyString(), eq(mf));

        assertThrows(AttachmentStorageException.class,
                () -> attachmentService.uploadAttachment("t1", mf, "u1", "user1"));

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void getAttachmentsByTicket_success_generatesPresignedUrls() {
        String ticketId = "t1";
        Attachment a1 = attachment("a1", ticketId, "f1.txt", "text/plain", "tickets/t1/1");
        Attachment a2 = attachment("a2", ticketId, "f2.txt", "text/plain", "tickets/t1/2");

        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId)).thenReturn(List.of(a1, a2));
        when(s3StorageService.generatePresignedUrl("tickets/t1/1")).thenReturn("u1");
        when(s3StorageService.generatePresignedUrl("tickets/t1/2")).thenReturn("u2");

        List<AttachmentDTO> dtos = attachmentService.getAttachmentsByTicket(ticketId);

        assertEquals(2, dtos.size());
        assertEquals("u1", dtos.get(0).downloadUrl());
        assertEquals("u2", dtos.get(1).downloadUrl());
        verify(s3StorageService, times(2)).generatePresignedUrl(anyString());
    }

    @Test
    void getAttachmentsByTicket_repoThrows_wrapsAttachmentFetchException() {
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc("t1"))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(AttachmentFetchException.class,
                () -> attachmentService.getAttachmentsByTicket("t1"));
    }

    @Test
    void getAttachment_success() {
        Attachment a = attachment("a1", "t1", "f.txt", "text/plain", "tickets/t1/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));
        when(s3StorageService.generatePresignedUrl("tickets/t1/1")).thenReturn("url");

        AttachmentDTO dto = attachmentService.getAttachment("a1");

        assertEquals("a1", dto.attachmentId());
        assertEquals("url", dto.downloadUrl());
    }

    @Test
    void getAttachment_notFound_wrapsAttachmentFetchException() {
        when(attachmentRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(AttachmentFetchException.class, () -> attachmentService.getAttachment("missing"));
    }

    @Test
    void downloadAttachment_success_returnsFileAndLogsDownload() {
        String ticketId = "t1";
        String attachmentId = "a1";
        String userId = "u1";

        Attachment a = attachment(attachmentId, ticketId, "file.txt", "text/plain", "tickets/t1/1");

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(a));
        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        when(s3StorageService.generatePresignedUrl("tickets/t1/1")).thenReturn("http://presigned");
        when(restTemplate.getForEntity("http://presigned", byte[].class))
                .thenReturn(ResponseEntity.ok("ABC".getBytes(StandardCharsets.UTF_8)));

        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<byte[]> resp = attachmentService.downloadAttachment(ticketId, attachmentId, userId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, resp.getHeaders().getContentType());
        assertNotNull(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals("ABC".getBytes(StandardCharsets.UTF_8), resp.getBody());

        // logDownload path
        verify(attachmentRepository, atLeast(1)).save(any(Attachment.class));
        assertEquals(1, a.getDownloadCount());
        assertEquals(userId, a.getLastDownloadedBy());
        assertNotNull(a.getLastDownloadedAt());
    }

    @Test
    void downloadAttachment_attachmentNotFound_throws() {
        when(attachmentRepository.findById("a1")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> attachmentService.downloadAttachment("t1", "a1", "u1"));
    }

    @Test
    void downloadAttachment_ticketMismatch_throws() {
        Attachment a = attachment("a1", "t-other", "file.txt", "text/plain", "tickets/t-other/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));
        assertThrows(RuntimeException.class, () -> attachmentService.downloadAttachment("t1", "a1", "u1"));
    }

    @Test
    void downloadAttachment_accessDenied_throws() {
        Attachment a = attachment("a1", "t1", "file.txt", "text/plain", "tickets/t1/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));
        when(ticketRepository.existsById("t1")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> attachmentService.downloadAttachment("t1", "a1", "u1"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void downloadAttachment_s3Non2xx_throws() {
        Attachment a = attachment("a1", "t1", "file.txt", "text/plain", "tickets/t1/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));
        when(ticketRepository.existsById("t1")).thenReturn(true);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("http://presigned");
        when(restTemplate.getForEntity("http://presigned", byte[].class))
                .thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body(new byte[]{1}));

        assertThrows(RuntimeException.class, () -> attachmentService.downloadAttachment("t1", "a1", "u1"));
    }

    @Test
    void downloadAttachment_s3EmptyBody_throws() {
        Attachment a = attachment("a1", "t1", "file.txt", "text/plain", "tickets/t1/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));
        when(ticketRepository.existsById("t1")).thenReturn(true);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("http://presigned");
        when(restTemplate.getForEntity("http://presigned", byte[].class))
                .thenReturn(ResponseEntity.ok(new byte[0]));

        assertThrows(RuntimeException.class, () -> attachmentService.downloadAttachment("t1", "a1", "u1"));
    }

    @Test
    void downloadAllAttachments_success_zipContainsEntries_andLogsEach() throws Exception {
        String ticketId = "t1";
        String userId = "u1";

        Attachment a1 = attachment("a1", ticketId, "f1.txt", "text/plain", "tickets/t1/1");
        Attachment a2 = attachment("a2", ticketId, "f2.txt", "text/plain", "tickets/t1/2");

        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId)).thenReturn(List.of(a1, a2));

        when(s3StorageService.generatePresignedUrl("tickets/t1/1")).thenReturn("u1");
        when(s3StorageService.generatePresignedUrl("tickets/t1/2")).thenReturn("u2");

        when(restTemplate.getForEntity("u1", byte[].class)).thenReturn(ResponseEntity.ok("A".getBytes()));
        when(restTemplate.getForEntity("u2", byte[].class)).thenReturn(ResponseEntity.ok("B".getBytes()));

        Map<String, Attachment> byId = new HashMap<>();
        byId.put("a1", a1);
        byId.put("a2", a2);
        when(attachmentRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(byId.get(inv.getArgument(0))));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<byte[]> resp = attachmentService.downloadAllAttachments(ticketId, userId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, resp.getHeaders().getContentType());
        assertNotNull(resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().length > 0);

        // verify zip has 2 entries
        int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(resp.getBody()))) {
            while (zis.getNextEntry() != null) entries++;
        }
        assertEquals(2, entries);

        assertEquals(1, a1.getDownloadCount());
        assertEquals(1, a2.getDownloadCount());
        verify(attachmentRepository, atLeast(2)).save(any(Attachment.class));
    }

    @Test
    void downloadAllAttachments_accessDenied_throws() {
        when(ticketRepository.existsById("t1")).thenReturn(false);
        assertThrows(RuntimeException.class, () -> attachmentService.downloadAllAttachments("t1", "u1"));
    }

    @Test
    void downloadAllAttachments_emptyList_throws() {
        when(ticketRepository.existsById("t1")).thenReturn(true);
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc("t1")).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> attachmentService.downloadAllAttachments("t1", "u1"));
    }

    @Test
    void downloadAllAttachments_zipSkipsBadFiles_stillReturnsZip() throws Exception {
        String ticketId = "t1";
        when(ticketRepository.existsById(ticketId)).thenReturn(true);

        Attachment good = attachment("a1", ticketId, "ok.txt", "text/plain", "tickets/t1/good");
        Attachment bad  = attachment("a2", ticketId, "bad.txt", "text/plain", "tickets/t1/bad");

        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId)).thenReturn(List.of(good, bad));

        when(s3StorageService.generatePresignedUrl("tickets/t1/good")).thenReturn("u-good");
        when(s3StorageService.generatePresignedUrl("tickets/t1/bad")).thenReturn("u-bad");

        when(restTemplate.getForEntity("u-good", byte[].class)).thenReturn(ResponseEntity.ok("OK".getBytes()));
        when(restTemplate.getForEntity("u-bad", byte[].class))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[]{1}));

        // logDownload uses findById, keep it safe (won't throw)
        when(attachmentRepository.findById(anyString())).thenReturn(Optional.of(good));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<byte[]> resp = attachmentService.downloadAllAttachments(ticketId, "u1");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());

        int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(resp.getBody()))) {
            while (zis.getNextEntry() != null) entries++;
        }
        assertEquals(1, entries); // only the good one should be inside zip
    }

    @Test
    void deleteAttachment_success_deletesAndDecrements() {
        String ticketId = "t1";
        String attachmentId = "a1";

        Attachment a = attachment(attachmentId, ticketId, "file.txt", "text/plain", "tickets/t1/1");
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(a));

        attachmentService.deleteAttachment(attachmentId, ticketId);

        verify(s3StorageService).deleteFile("tickets/t1/1");
        verify(attachmentRepository).deleteById(attachmentId);
        verify(ticketService).decrementAttachmentCount(ticketId);
    }

    @Test
    void deleteAttachment_ticketMismatch_throws() {
        Attachment a = attachment("a1", "t-other", "file.txt", "text/plain", "tickets/t-other/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));

        assertThrows(RuntimeException.class, () -> attachmentService.deleteAttachment("a1", "t1"));

        verify(attachmentRepository, never()).deleteById(anyString());
        verify(ticketService, never()).decrementAttachmentCount(anyString());
    }

    @Test
    void canUserAccessTicket_repoThrows_returnsFalse() {
        when(ticketRepository.existsById("t1")).thenThrow(new RuntimeException("db down"));
        assertFalse(attachmentService.canUserAccessTicket("t1", "u1"));
    }

    @Test
    void logDownload_success_incrementsAndSaves() {
        Attachment a = attachment("a1", "t1", "f.txt", "text/plain", "tickets/t1/1");
        when(attachmentRepository.findById("a1")).thenReturn(Optional.of(a));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

        attachmentService.logDownload("a1", "u9");

        assertEquals(1, a.getDownloadCount());
        assertEquals("u9", a.getLastDownloadedBy());
        assertNotNull(a.getLastDownloadedAt());
        verify(attachmentRepository).save(a);
    }

    @Test
    void logDownload_notFound_doesNotThrow() {
        when(attachmentRepository.findById("missing")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> attachmentService.logDownload("missing", "u1"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void getAttachmentCount_delegatesToRepo() {
        when(attachmentRepository.countByTicketId("t1")).thenReturn(5L);
        assertEquals(5L, attachmentService.getAttachmentCount("t1"));
    }

    @Test
    void deleteAllAttachmentsByTicket_success_skipsFailures() {
        String ticketId = "t1";

        Attachment a1 = attachment("a1", ticketId, "f1.txt", "text/plain", "k1");
        Attachment a2 = attachment("a2", ticketId, "f2.txt", "text/plain", "k2");

        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId)).thenReturn(List.of(a1, a2));

        // First deleteFile fails, second works
        doThrow(new RuntimeException("s3 down")).doNothing().when(s3StorageService).deleteFile(anyString());

        attachmentService.deleteAllAttachmentsByTicket(ticketId);

        verify(s3StorageService).deleteFile("k1");
        verify(s3StorageService).deleteFile("k2");
        // deleteById should be called only for a2 (a1 failed inside inner try)
        verify(attachmentRepository, never()).deleteById("a1");
        verify(attachmentRepository).deleteById("a2");
    }

    @Test
    void deleteAllAttachmentsByTicket_repoThrows_throwsRuntime() {
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc("t1"))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> attachmentService.deleteAllAttachmentsByTicket("t1"));
    }
}