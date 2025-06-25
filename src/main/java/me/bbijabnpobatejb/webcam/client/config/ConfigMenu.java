package me.bbijabnpobatejb.webcam.client.config;

import com.google.gson.GsonBuilder;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import lombok.val;
import me.bbijabnpobatejb.webcam.client.WebcamClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.bbijabnpobatejb.webcam.WebcamMod.MOD_ID;

public class ConfigMenu implements ModMenuApi {

    public static final MutableText TITLE = Text.translatable("config." + MOD_ID + ".title");

    @SerialEntry
    public static int fps = 30;
    @SerialEntry
    public static int compression = 15;
    @SerialEntry
    public static float windowScale = 1f;
    @SerialEntry
    public static int renderWidth = 200;
    @SerialEntry
    public static int renderHeight = 200;
    @SerialEntry
    public static boolean renderEnabled = true;
    @SerialEntry
    public static boolean previewEnabled = true;
    @SerialEntry
    public static ShapeMode previewShapeMode = ShapeMode.SQUARE;
    @SerialEntry
    public static boolean playerRenderEnabled = true;
    @SerialEntry
    public static RenderCorner renderCorner = RenderCorner.TOP_RIGHT;
    @SerialEntry
    public static int offsetX = 10;
    @SerialEntry
    public static int offsetY = 10;
    @SerialEntry
    public static RotationMode playerRotationMode = RotationMode.FIXED;
    @SerialEntry
    public static ShapeMode playerShapeMode = ShapeMode.SQUARE;
    @SerialEntry
    public static float playerOffsetX = 0;
    @SerialEntry
    public static float playerOffsetY = 0;
    @SerialEntry
    public static float playerOffsetZ = 0;
    @SerialEntry
    public static float playerRenderScale = 1f;

    public static ConfigClassHandler<ConfigMenu> HANDLER = ConfigClassHandler.createBuilder(ConfigMenu.class)
            .id(Identifier.of(MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json5"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .setJson5(true)
                    .build())
            .build();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YetAnotherConfigLib.createBuilder()
                .save(HANDLER::save)
                .title(TITLE)

                .category(ConfigCategory.createBuilder()
                        .name(trans("category.general"))
                        .tooltip(trans("tooltip.general"))
                        .group(OptionGroup.createBuilder()
                                .name(trans("group.preview"))
                                .option(boolOption("previewEnabled", () -> previewEnabled, v -> previewEnabled = v))
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(trans("group.preview_window"))
                                .description(desc("description.preview_window"))
                                .option(enumOption("previewShapeMode", ShapeMode.class, () -> previewShapeMode, v -> previewShapeMode = v, getPreviewHudWindow()))
                                .option(floatSlider("windowScale", 0f, 10f, .001f, () -> windowScale, v -> windowScale = v))
                                .option(enumOption("renderCorner", RenderCorner.class, () -> renderCorner, v -> renderCorner = v, desc("description.render_corner")))
                                .option(intSlider("offsetX", -500, 500, 1, () -> offsetX, v -> offsetX = v, desc("description.offsetX")))
                                .option(intSlider("offsetY", -500, 500, 1, () -> offsetY, v -> offsetY = v, desc("description.offsetY")))
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(trans("group.performance"))
                                .description(getPreviewHudWindow())
                                .option(intSlider("fps", 1, 120, 1, () -> fps, v -> fps = v, desc("description.fps")))
                                .option(intSlider("compression", 0, 99, 1, () -> compression, v -> compression = v, desc("description.compression")))
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(trans("group.misc"))
                                .option(boolOption("renderEnabled", () -> renderEnabled, v -> {
                                    renderEnabled = v;
                                    if (MinecraftClient.getInstance().player != null)
                                        WebcamClient.getInstance().getCameraManager().updateStateCamera();
                                }, desc("description.render")))
                                .build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(trans("category.camera"))
                        .tooltip(trans("tooltip.camera"))
                        .group(OptionGroup.createBuilder()
                                .name(trans("group.select_camera"))
                                .options(generateCams())
                                .build())
                        .build())

//                .category(ConfigCategory.createBuilder()
//                        .name(trans("category.quality"))
//                        .group(OptionGroup.createBuilder()
//                                .name(trans("group.select_quality"))
//                                .options(generateQuality())
//                                .build())
//                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(trans("category.player"))
                        .tooltip(trans("tooltip.player"))
                        .group(OptionGroup.createBuilder()
                                .name(trans("group.player_main"))
                                .description(getPreviewPlayer())
                                .option(boolOption("playerRenderEnabled", () -> playerRenderEnabled, v -> playerRenderEnabled = v, desc("description.playerRenderEnabled")))
                                .option(floatSlider("playerRenderScale", 0f, 10f, .001f, () -> playerRenderScale, v -> playerRenderScale = v,
                                        getPreviewPlayer()))
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(trans("group.modes"))
                                .description(getPreviewPlayer())
                                .option(enumOption("playerRotationMode", RotationMode.class, () -> playerRotationMode, v -> playerRotationMode = v, desc("description.rotation")))
                                .option(enumOption("playerShapeMode", ShapeMode.class, () -> playerShapeMode, v -> playerShapeMode = v, desc("description.shape")))
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(trans("group.offset"))
                                .description(getPreviewPlayer())
                                .option(floatSlider("playerOffsetX", -10f, 10f, .001f, () -> playerOffsetX, v -> playerOffsetX = v, getPreviewPlayer()))
                                .option(floatSlider("playerOffsetY", -10f, 10f, .001f, () -> playerOffsetY, v -> playerOffsetY = v, getPreviewPlayer()))
                                .option(floatSlider("playerOffsetZ", -10f, 10f, .001f, () -> playerOffsetZ, v -> playerOffsetZ = v, getPreviewPlayer()))
                                .build())
                        .build())

                .build()
                .generateScreen(parent);
    }

    // Helpers below

    OptionDescription desc(String key) {
        return OptionDescription.of(trans(key));
    }

    MutableText trans(String key) {
        return Text.translatable("config." + MOD_ID + "." + key);
    }

    OptionDescription getPreviewHudWindow() {
        return getPreviewHudWindow("");
    }

    OptionDescription getPreviewHudWindow(String description) {
        return OptionDescription.createBuilder()
                .text(Text.literal(description))
                .customImage(WebcamClient.getInstance().getHudRenderer())
                .build();
    }

    OptionDescription getPreviewPlayer() {
        return getPreviewPlayer("");
    }

    OptionDescription getPreviewPlayer(String description) {
        return OptionDescription.createBuilder()
                .text(Text.literal(description))
                .customImage(WebcamClient.getInstance().getTestPlayerRenderer())
                .build();
    }

    Option<Boolean> boolOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(trans(key))
                .binding(true, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    Option<Boolean> boolOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, OptionDescription desc) {
        return Option.<Boolean>createBuilder()
                .name(trans(key))
                .description(desc)
                .binding(true, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    <T extends Enum<T>> Option<T> enumOption(String key, Class<T> clazz, Supplier<T> getter, Consumer<T> setter, OptionDescription desc) {
        return Option.<T>createBuilder()
                .name(trans(key))
                .description(desc)
                .binding(clazz.getEnumConstants()[0], getter, setter)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(clazz))
                .build();
    }


    Option<Integer> intSlider(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, OptionDescription desc) {
        return Option.<Integer>createBuilder()
                .name(trans(key))
                .description(desc)
                .binding(getter.get(), getter, setter)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    Option<Float> floatSlider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, OptionDescription desc) {
        return Option.<Float>createBuilder()
                .name(trans(key))
                .description(desc)
                .binding(getter.get(), getter, setter)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    Option<Float> floatSlider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter) {
        return Option.<Float>createBuilder()
                .name(trans(key))
                .binding(getter.get(), getter, setter)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    private @NotNull Collection<? extends Option<?>> generateCams() {
        Collection<ButtonOption> list = new ArrayList<>();
        val cams = WebcamClient.getInstance().getCameraManager().getWebcamList();

        if (cams.isEmpty()) {
            list.add(ButtonOption.createBuilder().name(trans("empty")).action((yaclScreen, buttonOption) -> {
            }).build());
            return list;
        }
        for (String s : cams) {
            val button = ButtonOption.createBuilder()
                    .name(Text.literal(s))
                    .action((yaclScreen, buttonOption) -> {
                        if (MinecraftClient.getInstance().player == null) return;
                        WebcamClient.getInstance().getCameraManager().setWebcamByName(s);
                    }).build();
            list.add(button);
        }
        return list;
    }

    private @NotNull Collection<? extends Option<?>> generateQuality() {
        Collection<ButtonOption> list = new ArrayList<>();
        val webcam = WebcamClient.getInstance().getCameraManager().getCurrentWebcam();

        if (webcam == null) {
            list.add(ButtonOption.createBuilder().name(trans("empty")).action((yaclScreen, buttonOption) -> {
            }).build());
            return list;
        }

        for (Dimension viewSize : webcam.getViewSizes()) {
            val button = ButtonOption.createBuilder()
                    .name(Text.literal(viewSize.width + "x" + viewSize.height))
                    .action((yaclScreen, buttonOption) -> {
                        if (MinecraftClient.getInstance().player == null) return;
                        WebcamClient.getInstance().getCameraManager().setViewSize(viewSize);
                    }).build();
            list.add(button);
        }

        return list;
    }

    public enum RotationMode {
        ON_HEAD,
        FIXED
    }

    public enum ShapeMode {
        CIRCLE,
        SQUARE
    }

    public enum RenderCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}