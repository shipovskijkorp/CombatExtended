package com.shipovskijkorp.combatextended.client.gui;

import com.shipovskijkorp.combatextended.combat.compatibility.config.CombatCompatibilityConfig;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CompatibilityDecision;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CompatibilityDecisionSource;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CompatibilityListType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CombatExtendedCompatibilityConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 74;
    private static final int FOOTER_HEIGHT = 30;

    private final Screen parent;
    private Section section = Section.MODS;
    private CompatibilityListType selectedList = CompatibilityListType.NEUTRAL;
    private int page;
    private int rowsPerPage;
    private List<Row> rows = List.of();

    public CombatExtendedCompatibilityConfigScreen(Screen parent) {
        super(Text.translatable("screen.combatextended.compatibility.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        CombatCompatibilityConfig.load();

        rowsPerPage = Math.max(1, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
        rows = buildRows();
        int maxPage = getMaxPage();
        page = MathHelper.clamp(page, 0, maxPage);

        addTopButtons();
        addRowButtons();
        addFooterButtons();
    }

    private void addTopButtons() {
        int centerX = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.combatextended.compatibility.section.mods"), button -> {
            section = Section.MODS;
            page = 0;
            rebuild();
        }).dimensions(centerX - 155, 26, 75, 20).build()).active = section != Section.MODS;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.combatextended.compatibility.section.items"), button -> {
            section = Section.ITEMS;
            page = 0;
            rebuild();
        }).dimensions(centerX - 75, 26, 75, 20).build()).active = section != Section.ITEMS;

        addListButton(CompatibilityListType.WHITELIST, centerX + 10, 26);
        addListButton(CompatibilityListType.NEUTRAL, centerX + 88, 26);
        addListButton(CompatibilityListType.BLACKLIST, centerX + 166, 26);
    }

    private void addListButton(CompatibilityListType type, int x, int y) {
        ButtonWidget button = ButtonWidget.builder(getListName(type), ignored -> {
            selectedList = type;
            page = 0;
            rebuild();
        }).dimensions(x, y, 74, 20).build();
        button.active = selectedList != type;
        addDrawableChild(button);
    }

    private void addRowButtons() {
        int start = page * rowsPerPage;
        int end = Math.min(rows.size(), start + rowsPerPage);
        int y = HEADER_HEIGHT;

        for (int index = start; index < end; index++) {
            Row row = rows.get(index);
            int rowY = y + (index - start) * ROW_HEIGHT;

            addDrawableChild(ButtonWidget.builder(Text.literal("W"), button -> setRule(row, CompatibilityListType.WHITELIST))
                    .dimensions(width - 130, rowY + 1, 28, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("N"), button -> setRule(row, CompatibilityListType.NEUTRAL))
                    .dimensions(width - 98, rowY + 1, 28, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("B"), button -> setRule(row, CompatibilityListType.BLACKLIST))
                    .dimensions(width - 66, rowY + 1, 28, 20)
                    .build());

            if (row.kind == RowKind.ITEM && CombatCompatibilityConfig.getItemRule(row.id).isPresent()) {
                addDrawableChild(ButtonWidget.builder(Text.literal("R"), button -> {
                            CombatCompatibilityConfig.clearItemRule(row.id);
                            rebuild();
                        })
                        .dimensions(width - 34, rowY + 1, 22, 20)
                        .build());
            }
        }
    }

    private void addFooterButtons() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 50, height - 24, 100, 20)
                .build());

        ButtonWidget previous = ButtonWidget.builder(Text.literal("<"), button -> {
            page = Math.max(0, page - 1);
            rebuild();
        }).dimensions(10, height - 24, 28, 20).build();
        previous.active = page > 0;
        addDrawableChild(previous);

        ButtonWidget next = ButtonWidget.builder(Text.literal(">"), button -> {
            page = Math.min(getMaxPage(), page + 1);
            rebuild();
        }).dimensions(42, height - 24, 28, 20).build();
        next.active = page < getMaxPage();
        addDrawableChild(next);
    }

    private void setRule(Row row, CompatibilityListType type) {
        if (row.kind == RowKind.MOD) {
            CombatCompatibilityConfig.setModRule(row.modId, type);
        } else {
            CombatCompatibilityConfig.setItemRule(row.id, type);
        }

        rebuild();
    }

    private List<Row> buildRows() {
        if (section == Section.MODS) {
            return buildModRows();
        }

        return buildItemRows();
    }

    private List<Row> buildModRows() {
        List<Row> result = new ArrayList<>();

        FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(container -> container.getMetadata().getId()))
                .forEach(container -> {
                    String modId = container.getMetadata().getId();
                    CompatibilityListType type = CombatCompatibilityConfig.getModRule(modId)
                            .orElse(CompatibilityListType.NEUTRAL);
                    if (type != selectedList) {
                        return;
                    }

                    String displayName = container.getMetadata().getName();
                    if (displayName == null || displayName.isBlank()) {
                        displayName = modId;
                    }

                    result.add(Row.mod(modId, displayName, type));
                });

        return result;
    }

    private List<Row> buildItemRows() {
        List<Row> result = new ArrayList<>();

        Registries.ITEM.getIds().stream()
                .filter(id -> !id.getNamespace().equals("minecraft"))
                .sorted(Comparator.comparing(Identifier::toString))
                .forEach(id -> {
                    Item item = Registries.ITEM.get(id);
                    CompatibilityDecision decision = CombatCompatibilityConfig.resolve(id);
                    if (decision.type() != selectedList) {
                        return;
                    }

                    String modName = FabricLoader.getInstance().getModContainer(id.getNamespace())
                            .map(ModContainer::getMetadata)
                            .map(metadata -> {
                                String name = metadata.getName();
                                return name == null || name.isBlank() ? id.getNamespace() : name;
                            })
                            .orElse(id.getNamespace());

                    result.add(Row.item(id, new ItemStack(item), modName, decision));
                });

        return result;
    }

    private int getMaxPage() {
        if (rows.isEmpty()) {
            return 0;
        }

        return Math.max(0, (rows.size() - 1) / rowsPerPage);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.combatextended.compatibility.columns.actions"), width - 128, 58, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.combatextended.compatibility.page", page + 1, getMaxPage() + 1), 78, height - 19, 0xA0A0A0);

        int start = page * rowsPerPage;
        int end = Math.min(rows.size(), start + rowsPerPage);
        int y = HEADER_HEIGHT;

        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.combatextended.compatibility.empty"), width / 2, y + 24, 0xA0A0A0);
            return;
        }

        for (int index = start; index < end; index++) {
            Row row = rows.get(index);
            int rowY = y + (index - start) * ROW_HEIGHT;
            int textX = 16;

            if (row.stack != null) {
                context.drawItem(row.stack, textX, rowY + 2);
                textX += 22;
            }

            context.drawTextWithShadow(textRenderer, row.title(), textX, rowY + 2, row.titleColor());
            context.drawTextWithShadow(textRenderer, row.subtitle(), textX, rowY + 13, 0x808080);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && page > 0) {
            page--;
            rebuild();
            return true;
        }

        if (verticalAmount < 0 && page < getMaxPage()) {
            page++;
            rebuild();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private Text getListName(CompatibilityListType type) {
        return switch (type) {
            case WHITELIST -> Text.translatable("screen.combatextended.compatibility.list.white");
            case NEUTRAL -> Text.translatable("screen.combatextended.compatibility.list.neutral");
            case BLACKLIST -> Text.translatable("screen.combatextended.compatibility.list.black");
        };
    }

    private enum Section {
        MODS,
        ITEMS
    }

    private enum RowKind {
        MOD,
        ITEM
    }

    private record Row(
            RowKind kind,
            String modId,
            Identifier id,
            ItemStack stack,
            Text title,
            Text subtitle,
            CompatibilityDecision decision
    ) {
        private static Row mod(String modId, String name, CompatibilityListType type) {
            CompatibilityDecision decision = new CompatibilityDecision(type, CompatibilityDecisionSource.USER_MOD_RULE);
            return new Row(
                    RowKind.MOD,
                    modId,
                    Identifier.of(modId, "_mod"),
                    null,
                    Text.literal(name + " (" + modId + ")"),
                    Text.translatable("screen.combatextended.compatibility.source.mod_rule"),
                    decision
            );
        }

        private static Row item(Identifier id, ItemStack stack, String modName, CompatibilityDecision decision) {
            return new Row(
                    RowKind.ITEM,
                    id.getNamespace(),
                    id,
                    stack,
                    Text.literal(id.toString()),
                    Text.translatable(sourceTranslationKey(decision), modName),
                    decision
            );
        }

        private int titleColor() {
            return switch (decision.type()) {
                case WHITELIST -> 0xFFFF55;
                case NEUTRAL -> 0xAAAAAA;
                case BLACKLIST -> 0xFF5555;
            };
        }

        private static String sourceTranslationKey(CompatibilityDecision decision) {
            return switch (decision.source()) {
                case BUILT_IN_COMPATIBILITY -> "screen.combatextended.compatibility.source.built_in";
                case USER_ITEM_RULE -> "screen.combatextended.compatibility.source.item_rule";
                case USER_MOD_RULE -> "screen.combatextended.compatibility.source.inherited_mod_rule";
                case DEFAULT_NEUTRAL -> "screen.combatextended.compatibility.source.default_neutral";
            };
        }
    }
}
