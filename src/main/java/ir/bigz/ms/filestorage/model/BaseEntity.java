package ir.bigz.ms.filestorage.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.sql.Timestamp;

@MappedSuperclass
public abstract class BaseEntity <T extends Number> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected T id;
    protected Timestamp creationDate = new Timestamp(System.currentTimeMillis());
    protected Timestamp deletedDate;
    protected boolean deleted = false;

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public Timestamp getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Timestamp deletedDate) {
        this.deletedDate = deletedDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        if(deleted){
            this.setDeletedDate(new Timestamp(System.currentTimeMillis()));
        }
        this.deleted = deleted;
    }
}
