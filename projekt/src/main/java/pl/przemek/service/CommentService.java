package pl.przemek.service;


import pl.przemek.model.Comment;
import pl.przemek.model.Discovery;
import pl.przemek.repository.CommentRepository;
import pl.przemek.repository.DiscoveryRepository;

import javax.inject.Inject;
import java.util.List;

public class CommentService {

    @Inject
    private CommentRepository commentrepo;
    @Inject
    private DiscoveryRepository discrepo;

    public void addComment(Comment comment, long id){
        Discovery discvoery=discrepo.get(id);
        comment.setDiscvovery(discvoery);
        commentrepo.add(comment);
    }
    public List<Comment> getAllComment(){
        return commentrepo.getAll();
    }

    public List<Comment> getByDiscoveryName(String name){
        return commentrepo.getByDiscoveryName(name);

    }
    public List<Comment> getByDiscoveryId(long id){
        return commentrepo.getByDiscoveryId(id);
    }
}
