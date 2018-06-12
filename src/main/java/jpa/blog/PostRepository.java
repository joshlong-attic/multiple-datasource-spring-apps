package jpa.blog;

import org.springframework.data.jpa.repository.JpaRepository;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public interface PostRepository extends JpaRepository<Post, Long> {
}
