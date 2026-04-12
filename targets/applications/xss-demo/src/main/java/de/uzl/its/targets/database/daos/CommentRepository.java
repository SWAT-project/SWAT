package de.uzl.its.targets.database.daos;

import de.uzl.its.targets.database.models.Comment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, Long> {
    List<Comment> findByCountryName(String countryName);
} 