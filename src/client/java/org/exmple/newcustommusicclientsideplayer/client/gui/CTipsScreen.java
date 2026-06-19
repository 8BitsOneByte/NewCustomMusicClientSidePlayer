package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CTipsScreen extends Screen {
    private static final String TIP_KEY_PREFIX =
        "screen.custommusicclientsideplayer.main_menu.tip.";
    private static final Component TITLE =
        Component.translatable("screen.custommusicclientsideplayer.tips.title");
    private static final Component PREVIOUS_TIP =
        Component.translatable("screen.custommusicclientsideplayer.main_menu.previous_tip");
    private static final Component RANDOM_TIP =
        Component.translatable("screen.custommusicclientsideplayer.main_menu.random_tip");
    private static final Component NEXT_TIP =
        Component.translatable("screen.custommusicclientsideplayer.main_menu.next_tip");

    private static final int HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 36;
    private static final int CONTENT_SPACING = 8;
    private static final int CONTROL_SPACING = 6;
    private static final int TIP_CONTROL_BUTTON_WIDTH = 101;
    private static final int BACK_BUTTON_WIDTH = 210;
    private static final int TIP_AREA_MAX_WIDTH = 420;
    private static final int TIP_AREA_MAX_HEIGHT = 90;
    private static final int TIP_AREA_HORIZONTAL_MARGIN = 40;
    private static final int CONTROL_ROW_HEIGHT = 20;

    private final Screen parent;
    private final HeaderAndFooterLayout layout =
        new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

    private List<Component> tips;
    private int tipIndex;
    private FittingMultiLineTextWidget tipWidget;

    public CTipsScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.ensureTipsLoaded();
        this.layout.removeChildren();
        this.layout.addTitleHeader(this.title, this.font);

        LinearLayout content = LinearLayout.vertical().spacing(CONTENT_SPACING);
        content.defaultCellSetting().alignHorizontallyCenter();
        int tipAreaWidth = Math.min(
            TIP_AREA_MAX_WIDTH,
            Math.max(BACK_BUTTON_WIDTH, this.width - TIP_AREA_HORIZONTAL_MARGIN)
        );
        int tipAreaHeight = Math.max(
            this.font.lineHeight + 8,
            Math.min(
                TIP_AREA_MAX_HEIGHT,
                this.layout.getContentHeight() - CONTENT_SPACING - CONTROL_ROW_HEIGHT
            )
        );
        this.tipWidget = content.addChild(
            new FittingMultiLineTextWidget(
                0,
                0,
                tipAreaWidth,
                tipAreaHeight,
                this.currentTip(),
                this.font
            )
        );

        GridLayout controls = content.addChild(
            new GridLayout().spacing(CONTROL_SPACING)
        );
        controls.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper controlRow = controls.createRowHelper(3);
        controlRow.addChild(
            Button.builder(PREVIOUS_TIP, button -> this.showPreviousTip())
                .width(TIP_CONTROL_BUTTON_WIDTH)
                .build()
        );
        controlRow.addChild(
            Button.builder(RANDOM_TIP, button -> this.rollRandomTip())
                .width(TIP_CONTROL_BUTTON_WIDTH)
                .build()
        );
        controlRow.addChild(
            Button.builder(NEXT_TIP, button -> this.showNextTip())
                .width(TIP_CONTROL_BUTTON_WIDTH)
                .build()
        );
        this.layout.addToContents(
            content,
            settings -> settings.alignHorizontallyCenter().alignVerticallyMiddle()
        );

        this.layout.addToFooter(
            Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .width(BACK_BUTTON_WIDTH)
                .build()
        );

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private void showPreviousTip() {
        if (this.tips.isEmpty()) {
            this.showCurrentTip();
            return;
        }

        this.tipIndex = Math.floorMod(this.tipIndex - 1, this.tips.size());
        this.showCurrentTip();
    }

    private void rollRandomTip() {
        if (this.tips.size() > 1) {
            int nextIndex;
            do {
                nextIndex = ThreadLocalRandom.current().nextInt(this.tips.size());
            } while (nextIndex == this.tipIndex);
            this.tipIndex = nextIndex;
        } else {
            this.tipIndex = 0;
        }

        this.showCurrentTip();
    }

    private void showNextTip() {
        if (this.tips.isEmpty()) {
            this.showCurrentTip();
            return;
        }

        this.tipIndex = Math.floorMod(this.tipIndex + 1, this.tips.size());
        this.showCurrentTip();
    }

    private void showCurrentTip() {
        if (this.tipWidget != null) {
            this.tipWidget.setMessage(this.currentTip());
            this.tipWidget.setScrollAmount(0.0D);
        }
    }

    private Component currentTip() {
        if (this.tips.isEmpty()) {
            return Component.literal(" ");
        }

        return this.tips.get(this.tipIndex);
    }

    private void ensureTipsLoaded() {
        if (this.tips != null) {
            return;
        }

        List<Component> loaded = new ArrayList<>();
        Language language = Language.getInstance();
        for (int i = 1; ; i++) {
            String key = TIP_KEY_PREFIX + i;
            if (!language.has(key)) {
                break;
            }
            loaded.add(Component.translatable(key));
        }

        this.tips = List.copyOf(loaded);
        this.tipIndex = this.tips.isEmpty()
            ? 0
            : ThreadLocalRandom.current().nextInt(this.tips.size());
    }
}
