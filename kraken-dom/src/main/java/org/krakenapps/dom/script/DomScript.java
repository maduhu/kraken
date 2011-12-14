package org.krakenapps.dom.script;

import org.krakenapps.api.Script;
import org.krakenapps.api.ScriptArgument;
import org.krakenapps.api.ScriptContext;
import org.krakenapps.api.ScriptUsage;
import org.krakenapps.dom.api.ApplicationApi;
import org.krakenapps.dom.api.AreaApi;
import org.krakenapps.dom.api.GlobalConfigApi;
import org.krakenapps.dom.api.HostApi;
import org.krakenapps.dom.api.OrganizationApi;
import org.krakenapps.dom.api.ProgramApi;
import org.krakenapps.dom.api.RoleApi;
import org.krakenapps.dom.api.UserApi;
import org.krakenapps.dom.model.HostExtension;
import org.krakenapps.dom.model.HostType;
import org.krakenapps.dom.model.Program;
import org.krakenapps.dom.model.ProgramPack;
import org.krakenapps.dom.model.ProgramProfile;
import org.krakenapps.dom.model.Vendor;

public class DomScript implements Script {
	private ScriptContext context;

	private GlobalConfigApi globalConfigApi;
	private OrganizationApi orgApi;
	private UserApi userApi;
	private RoleApi roleApi;
	private ProgramApi programApi;
	private AreaApi areaApi;
	private HostApi hostApi;
	private ApplicationApi appApi;

	public DomScript(GlobalConfigApi globalConfigApi, OrganizationApi orgApi, UserApi userApi, RoleApi roleApi, ProgramApi programApi,
			AreaApi areaApi, HostApi hostApi, ApplicationApi appApi) {
		this.globalConfigApi = globalConfigApi;
		this.orgApi = orgApi;
		this.userApi = userApi;
		this.roleApi = roleApi;
		this.programApi = programApi;
		this.areaApi = areaApi;
		this.hostApi = hostApi;
		this.appApi = appApi;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void install(String[] args) {
		InitialSchema.generate(context, globalConfigApi, orgApi, roleApi, programApi, areaApi, userApi);
		context.println("install complete");
	}

	public void hostTypes(String[] args) {
		context.println("Host Types");
		context.println("------------");

		for (HostType t : hostApi.getHostTypes("localhost")) {
			context.println(t);
			for (HostExtension ext : t.getDefaultExtensions()) {
				context.println("\t" + ext);
			}
		}
	}

	@ScriptUsage(description = "add vendor", arguments = { @ScriptArgument(name = "domain", type = "string", description = "domain"),
			@ScriptArgument(name = "name", type = "string", description = "name") })
	public void addVendor(String[] args) {
		Vendor vendor = new Vendor();
		vendor.setName(args[1]);
		appApi.createVendor(args[0], vendor);
		context.println("added " + vendor.getGuid());
	}

	@ScriptUsage(description = "add host type", arguments = { @ScriptArgument(name = "domain", type = "string", description = ""),
			@ScriptArgument(name = "vendor guid", type = "string", description = ""),
			@ScriptArgument(name = "name", type = "string", description = ""),
			@ScriptArgument(name = "version", type = "string", description = "") })
	public void addHostType(String[] args) {
		Vendor vendor = appApi.getVendor(args[0], args[1]);

		HostType t = new HostType();
		t.setVendor(vendor);
		t.setName(args[2]);
		t.setVersion(args[3]);
		hostApi.createHostType(args[0], t);
		context.println("added " + t.getGuid());
	}

	@ScriptUsage(description = "add host extension", arguments = { @ScriptArgument(name = "domain", type = "string", description = ""),
			@ScriptArgument(name = "host type guid", type = "string", description = ""),
			@ScriptArgument(name = "type", type = "string", description = "host extension type"),
			@ScriptArgument(name = "ordinal", type = "string", description = "ordinal") })
	public void addHostExtension(String[] args) {
		String domain = args[0];
		String hostTypeGuid = args[1];
		String type = args[2];
		int ordinal = Integer.valueOf(args[3]);

		HostExtension ext = new HostExtension();
		ext.setType(type);
		ext.setOrd(ordinal);

		HostType t = hostApi.getHostType(domain, hostTypeGuid);
		t.getDefaultExtensions().add(ext);

		hostApi.updateHostType(domain, t);
		context.println("added");
	}

	@ScriptUsage(description = "list all programs", arguments = { @ScriptArgument(name = "domain", type = "string", description = "") })
	public void programs(String[] args) {
		context.println("Programs");
		context.println("----------");
		for (Program p : programApi.getPrograms(args[0])) {
			context.println(p);
		}
	}

	@ScriptUsage(description = "add new program", arguments = {
			@ScriptArgument(name = "domain", type = "string", description = "org domain"),
			@ScriptArgument(name = "package name", type = "string", description = "package name"),
			@ScriptArgument(name = "name", type = "string", description = "program name"),
			@ScriptArgument(name = "type", type = "string", description = "program type") })
	public void addProgram(String[] args) {
		String domain = args[0];
		String packName = args[1];
		String name = args[2];
		String type = args[3];

		Program p = new Program();
		p.setPackName(packName);
		p.setName(name);
		p.setTypeName(type);
		p.setVisible(true);
		programApi.createProgram(domain, p);

		ProgramProfile pp = programApi.getProgramProfile(domain, "all");
		pp.getPrograms().add(p);
		programApi.updateProgramProfile(domain, pp);

		context.println("added");
	}

	@ScriptUsage(description = "add new program", arguments = {
			@ScriptArgument(name = "domain", type = "string", description = "org domain"),
			@ScriptArgument(name = "package name", type = "string", description = "package name"),
			@ScriptArgument(name = "name", type = "string", description = "program name") })
	public void updateProgram(String[] args) {
		String domain = args[0];
		String packName = args[1];
		String name = args[2];
		Program program = programApi.findProgram(domain, packName, name);
		if (program == null) {
			context.println("not found");
			return;
		}

		try {
			context.print("description: " + program.getDescription() + " -> ");
			String description = context.readLine();
			if (!description.isEmpty())
				program.setDescription(description);

			context.print("type name: " + program.getDescription() + " -> ");
			String typeName = context.readLine();
			if (!typeName.isEmpty())
				program.setTypeName(typeName);

			context.print("visible: " + program.getDescription() + " -> ");
			String visible = context.readLine();
			if (!visible.isEmpty())
				program.setVisible(Boolean.parseBoolean(visible));

			context.print("seq: " + program.getDescription() + " -> ");
			String seq = context.readLine();
			if (!seq.isEmpty())
				program.setSeq(Integer.parseInt(seq));

			programApi.updateProgram(domain, program);
		} catch (InterruptedException e) {
			context.println("interrupted");
		}
	}

	@ScriptUsage(description = "remove program", arguments = {
			@ScriptArgument(name = "domain", type = "string", description = "org domain"),
			@ScriptArgument(name = "package name", type = "string", description = "package name"),
			@ScriptArgument(name = "program name", type = "string", description = "program name") })
	public void removeProgram(String[] args) {
		String domain = args[0];
		String packName = args[1];
		String programName = args[2];

		ProgramProfile pp = programApi.getProgramProfile(domain, "all");
		Program target = null;
		for (Program p : pp.getPrograms())
			if (p.getPackName().equals(packName) && p.getName().equals(programName))
				target = p;

		pp.getPrograms().remove(target);
		programApi.updateProgramProfile(domain, pp);

		programApi.removeProgram(domain, packName, programName);
		context.println("removed");
	}

	@ScriptUsage(description = "add new program pack", arguments = {
			@ScriptArgument(name = "domain", type = "string", description = "org domain"),
			@ScriptArgument(name = "pack name", type = "string", description = "pack name"),
			@ScriptArgument(name = "dll", type = "string", description = "dll") })
	public void addProgramPack(String[] args) {
		String domain = args[0];
		String packName = args[1];
		String dll = args[2];

		ProgramPack pack = new ProgramPack();
		pack.setName(packName);
		pack.setDll(dll);

		programApi.createProgramPack(domain, pack);
	}
}
