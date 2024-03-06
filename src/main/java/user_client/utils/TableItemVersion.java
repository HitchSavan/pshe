package user_client.utils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import patcher.remote_api.entities.Version;

public class TableItemVersion {
    private final SimpleStringProperty versionString;
    private final SimpleStringProperty createdAt;
    private final SimpleIntegerProperty filesCount;
    private final SimpleIntegerProperty totalSize;
    @Getter
    private final Version version;

    public TableItemVersion(Version version) {
        this.versionString = new SimpleStringProperty(version.getVersionString());
        this.createdAt = new SimpleStringProperty(version.getCreatedAt());
        this.filesCount = new SimpleIntegerProperty(version.getFilesCount());
        this.totalSize = new SimpleIntegerProperty(version.getTotalSize());
        this.version = version;
    }

    public void setVersionString(String val) {
        this.versionString.set(val);
    }
    public void setCreatedAt(String val) {
        this.createdAt.set(val);
    }
    public void setFilesCount(Integer val) {
        this.filesCount.set(val);
    }
    public void setTotalSize(Integer val) {
        this.totalSize.set(val);
    }

    public String getVersionString() {
        return this.versionString.get();
    }
    public String getCreatedAt() {
        return this.createdAt.get();
    }
    public Integer getFilesCount() {
        return this.filesCount.get();
    }
    public Integer getTotalSize() {
        return this.totalSize.get();
    }
}
