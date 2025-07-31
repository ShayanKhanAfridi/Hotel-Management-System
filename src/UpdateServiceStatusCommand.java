public class UpdateServiceStatusCommand implements ServiceCommand {
    private Services services;

    public UpdateServiceStatusCommand(Services services) {
        this.services = services;
    }

    @Override
    public void execute() {
        services.updateServiceStatus();
    }
}
