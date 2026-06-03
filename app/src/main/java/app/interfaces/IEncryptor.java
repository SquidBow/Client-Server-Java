package app.interfaces;

import app.helpers.Message;

public interface IEncryptor {
    byte[] encrypt(Message message);
}
