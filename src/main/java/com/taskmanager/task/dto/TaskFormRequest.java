package com.taskmanager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Request object for submitting task form data")
public class TaskFormRequest {

    @Schema(description = "Form data as key-value pairs", example = "{\"reason\": \"Vacation\", \"status\": \"approve\"}", required = true)
    private Map<String, Object> formData;

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }
}