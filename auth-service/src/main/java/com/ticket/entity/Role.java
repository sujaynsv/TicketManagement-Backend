package com.ticket.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name="roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
	@Id
	@Column(name="role_id")
	private UUID roleId;
	
	@Column(name="role_name", unique=true, nullable=false, length=50)
	private String roleName;
	
	@Column(name="description", length=255)
	private String description;
	
	@Column(name="created_at")
	private LocalDateTime createdAt;
	
	@PrePersist
	protected void onCreate() {
		if(roleId==null) {
			roleId=UUID.randomUUID();
		}
		if(createdAt==null) {
			createdAt=LocalDateTime.now();
		}
	}
}
