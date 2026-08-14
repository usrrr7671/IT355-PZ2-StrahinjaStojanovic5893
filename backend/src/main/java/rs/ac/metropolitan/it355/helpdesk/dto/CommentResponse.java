package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long ticketId,
        UserSummary author,
        String content,
        boolean internal,
        LocalDateTime createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                UserSummary.from(comment.getAuthor()),
                comment.getContent(),
                comment.isInternal(),
                comment.getCreatedAt());
    }
}
