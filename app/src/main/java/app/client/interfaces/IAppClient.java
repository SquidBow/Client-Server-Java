package app.client.interfaces;

import app.generic.helpers.Message;

public interface IAppClient {
    public Message sendRequest(Message message) throws Exception;
}
