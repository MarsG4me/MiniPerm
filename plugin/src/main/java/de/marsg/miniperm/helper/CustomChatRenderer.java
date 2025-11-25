package de.marsg.miniperm.helper;

import org.bukkit.entity.Player;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CustomChatRenderer implements ChatRenderer {

    private final Component prefix;

    public CustomChatRenderer(String prefix) {
        this.prefix = Component.text(prefix);
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {

        /*
         * The custom chat message format used to display the groups prefix of the
         * sender infront of their name
         */
        Component nameComponent = Component.text()
                .append(Component.text(" <"))
                .append(sourceDisplayName)
                .append(Component.text("> "))
                .color(NamedTextColor.WHITE)
                .build();

        return Component.text()
                .append(prefix)
                .append(nameComponent)
                .append(message)
                .build();
    }

}
