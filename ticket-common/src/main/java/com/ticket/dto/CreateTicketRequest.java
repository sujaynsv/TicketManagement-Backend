package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateTicketRequest {
    
    @NotBlank(message = "Title is required")
    // @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    // @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    // @NotBlank(message = "Priority is required")
    // private String priority;
    
    private List<String> tags;
    
    // Constructors
    public CreateTicketRequest() {}
    
    public CreateTicketRequest(String title, String description, String category, 
                               List<String> tags) {
        this.title = title;
        this.description = description;
        this.category = category;
        // this.priority = priority;
        this.tags = tags;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    // public String getPriority() {
    //     return priority;
    // }
    
    // public void setPriority(String priority) {
    //     this.priority = priority;
    // }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
