package app.generic.interfaces;

import app.generic.helpers.Message;

public interface IEncryptor {
    byte[] encrypt(Message message);
}
