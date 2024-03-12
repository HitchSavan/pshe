package user_client.utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import patcher.remote_api.entities.VersionEntity;

public class HistoryTableItem {
    private final SimpleStringProperty versionString;
    private final SimpleStringProperty createdAt;
    private final SimpleLongProperty filesCount;
    private final SimpleLongProperty totalSize;
    private final SimpleBooleanProperty isRoot;
    @Getter
    private final VersionEntity version;

    public HistoryTableItem(VersionEntity version) {
        this.versionString = new SimpleStringProperty(version.getVersionString());
        this.createdAt = new SimpleStringProperty(version.getCreatedAt());
        this.filesCount = new SimpleLongProperty(version.getFilesCount());
        this.totalSize = new SimpleLongProperty(version.getTotalSize());
        this.isRoot = new SimpleBooleanProperty(version.isRoot());
        this.version = version;
    }

    public void setVersionString(String val) {
        this.versionString.set(val);
    }
    public void setCreatedAt(String val) {
        this.createdAt.set(val);
    }
    public void setFilesCount(Long val) {
        this.filesCount.set(val);
    }
    public void setTotalSize(Long val) {
        this.totalSize.set(val);
    }
    public void setIsRoot(boolean val) {
        this.isRoot.set(val);
    }

    public String getVersionString() {
        return this.versionString.get();
    }
    public String getCreatedAt() {
        return this.createdAt.get();
    }
    public Long getFilesCount() {
        return this.filesCount.get();
    }
    public Long getTotalSize() {
        return this.totalSize.get();
    }
    public boolean getIsRoot() {
        return this.isRoot.get();
    }
}
