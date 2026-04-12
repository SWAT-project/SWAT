package de.uzl.its.targets.database.controller;

import de.uzl.its.targets.database.daos.CommentRepository;
import de.uzl.its.targets.database.models.Comment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentRepository commentRepository;

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @PostMapping("/add")
    @ResponseBody
    public String addComment(@RequestParam String countryName, @RequestParam String content) {
        countryName =  designatedSource(countryName); // Designated source for taint
        content =  designatedSource(content); // Designated source for taint
        Comment comment = new Comment();
        comment.setCountryName(countryName);
        if(content.contains("<script>")){
            return "Invalid content: script tags are not allowed"; // Basic validation to prevent script injection
        }
        comment.setContent(content); // Vulnerable: No input sanitization

        commentRepository.save(comment); // Sink
        return "Comment added successfully";
    }

    @GetMapping("/view/{countryName}")
    @ResponseBody
    public String viewComments(@PathVariable String countryName) {
        List<Comment> comments = commentRepository.findByCountryName(countryName);
        StringBuilder response = new StringBuilder();
        response.append("<html><body>");
        response.append("<h2>Comments for ").append(countryName).append("</h2>");

        for (Comment comment : comments) {
            response.append("<div class='comment'>");
            response.append(comment.getContent()); // Vulnerable: No output encoding
            response.append("</div>");
        }

        response.append("</body></html>");
        return response.toString();
    }

    private String designatedSource(String taintable) {
        return taintable; // This method is a designated source for taint
    }
}
