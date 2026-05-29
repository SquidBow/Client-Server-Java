package app.logic;

public interface IDecryptor {
    //Why tf does this return void? It is supposed to be Message
    void decrypt(byte[] message);
}
