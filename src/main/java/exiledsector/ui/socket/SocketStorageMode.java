package exiledsector.ui.socket;

public sealed interface SocketStorageMode permits SocketStorageMode.Manage {

    record Manage() implements SocketStorageMode {
    }
}
