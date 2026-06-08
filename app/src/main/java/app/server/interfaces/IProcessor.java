package app.server.interfaces;

import app.generic.helpers.Message;

public interface IProcessor {
    void process(Message message);
}
