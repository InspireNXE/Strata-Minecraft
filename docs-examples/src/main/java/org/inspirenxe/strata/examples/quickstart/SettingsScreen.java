package org.inspirenxe.strata.examples.quickstart;

// --8<-- [start:screen]
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.TextBox;

/**
 * A small form: a text box, a button and a status line, centered on the screen.
 */
public final class SettingsScreen implements Screen {

    @Override
    public Component<?> build(final ComponentFactory factory) {
        final Label status = factory.label("Ready");

        final TextBox name = factory.textBox().setMaxLength(32);
        name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final Button save = factory.button("Save")
                .onAction(() -> status.setText("Saved " + name.text()));

        final StackPanel column = factory.stackPanel()
                .setSpacing(4)
                .setPadding(8)
                .add(factory.label("Your name"))
                .add(name)
                .add(save)
                .add(status);
        column.setWidth(200);
        column.setBackground(Paint.solid(0xFF2B2D31));
        column.setAnchor(Anchor.CENTER);

        return factory.anchorPanel().add(column).setAnchor(Anchor.FILL);
    }
}
// --8<-- [end:screen]
