public class RequestServiceCommand implements ServiceCommand {
    private Services services;

    public RequestServiceCommand(Services services) {
        this.services = services;
    }

    @Override
    public void execute() {
        services.requestService();
    }
}
