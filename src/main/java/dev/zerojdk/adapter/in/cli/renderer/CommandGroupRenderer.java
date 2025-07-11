package dev.zerojdk.adapter.in.cli.renderer;

import picocli.CommandLine;

import java.util.List;
import java.util.Map;

import static picocli.CommandLine.Model.UsageMessageSpec.*;

public class CommandGroupRenderer implements CommandLine.IHelpSectionRenderer {
    private final Map<String, List<String>> sections;

    public CommandGroupRenderer(Map<String, List<String>> sections) {
        this.sections = sections;
    }

    public void apply(CommandLine commandLine) {
        commandLine.getHelpSectionMap().remove(SECTION_KEY_COMMAND_LIST_HEADING);
        commandLine.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, this);
    }

    @Override
    public String render(CommandLine.Help help) {
        if (help.commandSpec().subcommands().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        sections.forEach((key, value) -> result.append(renderSection(key, value, help)));
        return result.toString();
    }

    private String renderSection(String sectionHeading, List<String> cmdNames, CommandLine.Help help) {
        CommandLine.Help.TextTable textTable = createTextTable(help);

        for (String name : cmdNames) {
            CommandLine.Model.CommandSpec sub = help.commandSpec().subcommands().get(name).getCommandSpec();

            // create comma-separated list of command name and aliases
            String names = sub.names().toString();
            names = names.substring(1, names.length() - 1); // remove leading '[' and trailing ']'

            // description may contain line separators; use Text::splitLines to handle this
            String description = description(sub.usageMessage());
            CommandLine.Help.Ansi.Text[] lines = help.colorScheme().text(String.format(description)).splitLines();

            for (int i = 0; i < lines.length; i++) {
                CommandLine.Help.Ansi.Text cmdNamesText = help.colorScheme().commandText(i == 0 ? names : "");
                textTable.addRowValues(cmdNamesText, lines[i]);
            }
        }
        return help.createHeading(sectionHeading) + textTable.toString();
    }

    private CommandLine.Help.TextTable createTextTable(CommandLine.Help help) {
        CommandLine.Model.CommandSpec spec = help.commandSpec();
        // prepare layout: two columns
        // the left column overflows, the right column wraps if text is too long
        int commandLength = maxLength(spec.subcommands(), 37);
        CommandLine.Help.TextTable textTable = CommandLine.Help.TextTable.forColumns(help.colorScheme(),
            new CommandLine.Help.Column(commandLength + 2, 2, CommandLine.Help.Column.Overflow.SPAN),
            new CommandLine.Help.Column(spec.usageMessage().width() - (commandLength + 2), 2, CommandLine.Help.Column.Overflow.WRAP));
        textTable.setAdjustLineBreaksForWideCJKCharacters(spec.usageMessage().adjustLineBreaksForWideCJKCharacters());
        return textTable;
    }

    private int maxLength(Map<String, CommandLine> subcommands, int max) {
        int result = subcommands.values().stream().map(cmd -> cmd.getCommandSpec().names().toString().length() - 2).max(Integer::compareTo).get();
        return Math.min(max, result);
    }

    private String description(CommandLine.Model.UsageMessageSpec usageMessage) {
        if (usageMessage.header().length > 0) {
            return usageMessage.header()[0];
        }
        if (usageMessage.description().length > 0) {
            return usageMessage.description()[0];
        }
        return "";
    }
}
