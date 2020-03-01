package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.api.JDA;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Command
public class ShardsCommand implements BaseCommand {

    private static final int SHARDS_PER_MESSAGE = 30;

    @Override
    public String getTrigger() {
        return "shards";
    }

    @Nonnull
    @Override
    public String help() {
        return "Show status of all shards.";
    }

    @Override
    @CheckReturnValue
    public boolean execute(CommandContext context) {
        StringBuilder sb = new StringBuilder("```diff\n");
        List<String> messages = new ArrayList<>();
        List<JDA> sorted = new ArrayList<>(requireNonNull(context.getJda().getShardManager()).getShards());
        sorted.sort(Comparator.comparingInt(o -> o.getShardInfo().getShardId()));
        int i = 0;
        for (JDA jda : sorted) {
            sb.append(jda.getStatus() == JDA.Status.CONNECTED ? "+" : "-")
                    .append(" ")
                    .append(jda.getShardInfo().getShardString())
                    .append(" ")
                    .append(jda.getStatus())
                    .append(" -- Guilds: ")
                    .append(String.format("%04d", jda.getGuildCache().size()))
                    .append(" -- Users: ")
                    .append(jda.getUserCache().size())
                    .append("\n");
            if (++i % SHARDS_PER_MESSAGE == 0 || i == sorted.size()) {
                sb.append("```");
                messages.add(sb.toString());
                sb = new StringBuilder("```diff\n");
            }
        }
        messages.forEach(context::reply);
        return true;
    }
}
