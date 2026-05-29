package app.logic;

import app.logic.Context;
import app.logic.LogicTuple;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Processor implements IProcessor, Runnable {

    private QueueManager queueManager;
    private Storage storage;
    private Context context;

    public Processor(QueueManager queueManager, Storage storage) {
        this.queueManager = queueManager;
        this.storage = storage;
    }

    public void run() {
        try {
            while (true) {
                LogicTuple<Message> in = queueManager.processor_queue.take();
                context = in.context;

                try {
                    process(in.data);
                } catch (Exception e) {}
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void process(Message message) {
        Message message2 = new Message();
        message2.command_id = message.command_id;
        message2.user_id = message.user_id;
        message2.message = "OK";

        // Maybe add auth to this when we execute command check for the right
        if (message.command_id == 1) {
            // Get the item cause, I mean, that is what you sometimes have to do, yeah, imagine, crazy, that's just incredible, truly an unforgettable experience
            int quantity = storage.item_table.getOrDefault(message.message, -1);

            if (quantity == -1) throw new IllegalArgumentException(
                "Item doesn't exist."
            );

            message2.message = String.valueOf(quantity);
        } else if (message.command_id == 2) {
            String[] command_arguments = message.message.split(":");

            if (command_arguments.length > 2) {
                throw new IllegalArgumentException(
                    "The command takes only 2 arguments"
                );
            }

            storage.item_table.merge(
                command_arguments[0],
                -Integer.parseInt(command_arguments[1]),
                Integer::sum
            );
        } else if (message.command_id == 3) {
            String[] command_arguments = message.message.split(":");

            if (command_arguments.length > 2) {
                throw new IllegalArgumentException(
                    "The command takes only 2 arguments"
                );
            }

            storage.item_table.merge(
                command_arguments[0],
                Integer.parseInt(command_arguments[1]),
                Integer::sum
            );
        } else if (message.command_id == 4) {
            storage.groups.putIfAbsent(
                message.message,
                ConcurrentHashMap.newKeySet()
            );
        } else if (message.command_id == 5) {
            String[] command_arguments = message.message.split(":");

            if (command_arguments.length > 2) {
                throw new IllegalArgumentException(
                    "The command takes only 2 arguments"
                );
            }

            Set<String> groups = storage.groups.get(command_arguments[0]);

            if (groups == null) throw new NoSuchElementException(
                "Group: " + command_arguments[0] + " doesn't exist."
            );

            groups.add(command_arguments[1]);
        } else if (message.command_id == 6) {
            String[] command_arguments = message.message.split(":");

            if (command_arguments.length > 2) {
                throw new IllegalArgumentException(
                    "The command takes only 2 arguments"
                );
            }

            storage.price_table.put(
                command_arguments[0],
                Integer.parseInt(command_arguments[1])
            );
        }

        System.out.println(
            "Processed message:\nCommand_id: " +
                message.command_id +
                "\nUser_id: " +
                message.user_id +
                "\nMessage: " +
                message.message
        );

        System.out.println(
            "Storage State -> Items: " +
                storage.item_table +
                ". Prices: " +
                storage.price_table
        );

        try {
            queueManager.encrypt_queue.put(new LogicTuple<>(message2, context));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
