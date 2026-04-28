package by.onton.relay;

class PlatformContact {
    final byte platform;
    final Contact contact;

    PlatformContact(byte platform, Contact contact) {
        this.platform = platform;
        this.contact = contact;
    }
}
