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

public class CombatExtendedCompatibilityConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private static final int HEADER_HEIGHT = 76;
    private static final int FOOTER_HEIGHT = 30;
    private static final String COMPATIBILITY_MARK = "✓";

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

            ButtonWidget white = ButtonWidget.builder(Text.literal("W"), button -> setRule(row, CompatibilityListType.WHITELIST))
                    .dimensions(width - 130, rowY + 2, 28, 20)
                    .build();
            ButtonWidget neutral = ButtonWidget.builder(Text.literal("N"), button -> setRule(row, CompatibilityListType.NEUTRAL))
                    .dimensions(width - 98, rowY + 2, 28, 20)
                    .build();
            ButtonWidget black = ButtonWidget.builder(Text.literal("B"), button -> setRule(row, CompatibilityListType.BLACKLIST))
                    .dimensions(width - 66, rowY + 2, 28, 20)
                    .build();

            boolean lockedByInternalCompatibility = row.hasRegisteredCompatibility || row.internalNeutralItem;
            if (lockedByInternalCompatibility && !row.decision.isBlacklisted()) {
                white.active = false;
                neutral.active = false;
            } else {
                white.active = row.decision.type() != CompatibilityListType.WHITELIST;
                neutral.active = row.decision.type() != CompatibilityListType.NEUTRAL;
            }
            black.active = row.decision.type() != CompatibilityListType.BLACKLIST;

            addDrawableChild(white);
            addDrawableChild(neutral);
            addDrawableChild(black);

            if (row.kind == RowKind.ITEM && CombatCompatibilityConfig.getItemRule(row.id).isPresent()) {
                addDrawableChild(ButtonWidget.builder(Text.literal("R"), button -> {
                            CombatCompatibilityConfig.clearItemRule(row.id);
                            rebuild();
                        })
                        .dimensions(width - 34, rowY + 2, 22, 20)
                        .build());
            } else if (row.kind == RowKind.MOD && CombatCompatibilityConfig.getModRule(row.modId).isPresent()) {
                addDrawableChild(ButtonWidget.builder(Text.literal("R"), button -> {
                            CombatCompatibilityConfig.clearModRule(row.modId);
                            rebuild();
                        })
                        .dimensions(width - 34, rowY + 2, 22, 20)
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
        boolean lockedByInternalCompatibility = row.hasRegisteredCompatibility || row.internalNeutralItem;
        if (lockedByInternalCompatibility && type != CompatibilityListType.BLACKLIST) {
            if (row.kind == RowKind.MOD) {
                CombatCompatibilityConfig.clearModRule(row.modId);
            } else {
                CombatCompatibilityConfig.clearItemRule(row.id);
            }
            rebuild();
            return;
        }

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
                    CompatibilityDecision decision = CombatCompatibilityConfig.resolveMod(modId);
                    if (decision.type() != selectedList) {
                        return;
                    }

                    String displayName = container.getMetadata().getName();
                    if (displayName == null || displayName.isBlank()) {
                        displayName = modId;
                    }

                    result.add(Row.mod(modId, displayName, decision, CombatCompatibilityConfig.hasRegisteredCompatibility(modId)));
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

                    result.add(Row.item(
                            id,
                            new ItemStack(item),
                            modName,
                            decision,
                            CombatCompatibilityConfig.hasRegisteredCompatibility(id),
                            CombatCompatibilityConfig.isInternalNeutralItem(id)
                    ));
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

            if ((index & 1) == 0) {
                context.fill(8, rowY, width - 8, rowY + ROW_HEIGHT - 1, 0x22000000);
            }

            if (row.stack != null) {
                context.drawItem(row.stack, textX, rowY + 4);
                textX += 22;
            }

            int maxTextWidth = Math.max(40, width - textX - 180);
            Text titleText = textRenderer.getWidth(row.title()) > maxTextWidth
                    ? Text.literal(textRenderer.trimToWidth(row.title().getString(), maxTextWidth - textRenderer.getWidth("...")) + "...")
                    : row.title();
            Text subtitleText = textRenderer.getWidth(row.subtitle()) > maxTextWidth
                    ? Text.literal(textRenderer.trimToWidth(row.subtitle().getString(), maxTextWidth - textRenderer.getWidth("...")) + "...")
                    : row.subtitle();

            context.drawTextWithShadow(textRenderer, titleText, textX, rowY + 3, row.titleColor());
            context.drawTextWithShadow(textRenderer, subtitleText, textX, rowY + 15, 0x808080);

            if (row.hasRegisteredCompatibility) {
                int iconX = width - 158;
                int iconY = rowY + 8;
                context.drawTextWithShadow(textRenderer, Text.literal(COMPATIBILITY_MARK), iconX, iconY, 0x55FF55);
                if (mouseX >= iconX - 2 && mouseX <= iconX + 12 && mouseY >= iconY - 2 && mouseY <= iconY + 12) {
                    context.drawTooltip(textRenderer, Text.translatable("screen.combatextended.compatibility.compatible_hint"), mouseX, mouseY);
                }
            }
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
            CompatibilityDecision decision,
            boolean hasRegisteredCompatibility,
            boolean internalNeutralItem
    ) {
        private static Row mod(String modId, String name, CompatibilityDecision decision, boolean hasRegisteredCompatibility) {
            return new Row(
                    RowKind.MOD,
                    modId,
                    Identifier.of(modId, "_mod"),
                    null,
                    Text.literal(name),
                    Text.literal("modid: " + modId + " | ").append(Text.translatable(sourceTranslationKey(decision))),
                    decision,
                    hasRegisteredCompatibility,
                    false
            );
        }

        private static Row item(
                Identifier id,
                ItemStack stack,
                String modName,
                CompatibilityDecision decision,
                boolean hasRegisteredCompatibility,
                boolean internalNeutralItem
        ) {
            return new Row(
                    RowKind.ITEM,
                    id.getNamespace(),
                    id,
                    stack,
                    stack.getName(),
                    Text.literal("item: " + id + " | mod: " + modName + " (" + id.getNamespace() + ") | ")
                            .append(Text.translatable(sourceTranslationKey(decision))),
                    decision,
                    hasRegisteredCompatibility,
                    internalNeutralItem
            );
        }

        private int titleColor() {
            if (decision.isBuiltInCompatible()) {
                return 0x55FF55;
            }

            return switch (decision.type()) {
                case WHITELIST -> 0xFFFF55;
                case NEUTRAL -> 0xAAAAAA;
                case BLACKLIST -> 0xFF5555;
            };
        }

        private static String sourceTranslationKey(CompatibilityDecision decision) {
            return switch (decision.source()) {
                case BUILT_IN_COMPATIBILITY -> "screen.combatextended.compatibility.source.built_in";
                case INTERNAL_NEUTRAL_ITEM -> "screen.combatextended.compatibility.source.internal_neutral_item";
                case USER_ITEM_RULE -> "screen.combatextended.compatibility.source.item_rule";
                case USER_MOD_RULE -> "screen.combatextended.compatibility.source.mod_rule";
                case DEFAULT_NEUTRAL -> "screen.combatextended.compatibility.source.default_neutral";
            };
        }
    }
}
