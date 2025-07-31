
public class ServiceInvoker {

    private ServiceCommand command;

    public void setCommand(ServiceCommand command) {
        this.command = command;
    }

    public void processRequest() {
        if (command != null) {
            command.execute();
        }
    }
}
