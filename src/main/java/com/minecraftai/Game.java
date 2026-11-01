package com.minecraftai;

import com.minecraftai.core.*;
import com.minecraftai.entities.SulfurCube;
import com.minecraftai.gui.*;
import com.minecraftai.renderer.*;
import com.minecraftai.blocks.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    public enum GameState {
        MAIN_MENU,
        PLAY_MENU,
        OPTIONS_MENU,
        KEYBINDS_MENU,
        PAUSE_MENU,
        IN_GAME,
        INVENTORY,
        DEATH,
        CRAFTING_TABLE,
        FURNACE,
        LOADING
    }

    private long window;
    private Player player;
    private World world;
    private MainMenu mainMenu;
    private PlayMenu playMenu;
    private OptionsMenu optionsMenu;
    private KeybindsMenu keybindsMenu;
    private LoadingScreen loadingScreen;
    private PauseMenu pauseMenu;
    private Hotbar hotbar;
    private InventoryMenu inventoryMenu;
    private CraftingTableMenu craftingTableMenu;
    private FurnaceMenu furnaceMenu;
    private DeathMenu deathMenu;
    private GameState currentState;
    private volatile boolean worldInitializationPending;
    private volatile Exception worldLoadFailure;
    private double lastX, lastY;
    private float currentFov = 70.0f;
    private int cameraMode = 0;
    private float lastDeltaTime = 0.016f;
    private boolean showDebug = false;
    private double oreVisionTimer = 0.0;
    private int fps = 0;
    private int frameCount = 0;
    private double fpsTimer = 0.0;
    private float timeOfDay = 6000.0f;
    private int sunTextureID;
    private int moonTextureID;
    private int[] destroyStageTextureIDs = new int[10];

    public static boolean COMMANDS_ENABLED = false;
    public static boolean SWAP_MOUSE_BUTTONS = false;
    public static boolean GAMMA_FULL_BRIGHT = false;
    public static float HOTBAR_SCALE = 0.5f;
    public static float CROSSHAIR_SIZE = 8.0f;

    // Keybinds
    public static int KEY_FORWARD = GLFW_KEY_W;
    public static int KEY_BACK = GLFW_KEY_S;
    public static int KEY_LEFT = GLFW_KEY_A;
    public static int KEY_RIGHT = GLFW_KEY_D;
    public static int KEY_JUMP = GLFW_KEY_SPACE;
    public static int KEY_INVENTORY = GLFW_KEY_E;
    public static int KEY_DROP = GLFW_KEY_Q;

    public boolean isCommandConsoleOpen = false;
    private String commandInput = "";
    private boolean isItemPickerOpen = false;
    private int itemPickerCategory = 2;
    private int itemPickerAmount = 1;

    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }

    public void run() {
        init();
        loop();
        if (world != null && player != null && world.getWorldName() != null) {
            WorldSaveManager.saveWorld(world, player, timeOfDay, world.getWorldName());
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");
        window = glfwCreateWindow(1280, 720, "Minecraft AI", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Window creation failed");
        setWindowIcon(window, "assets/textures/misc/icon.png");
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);
        lastX = xpos[0];
        lastY = ypos[0];

        glfwSetKeyCallback(window, this::keyCallback);
        glfwSetCursorPosCallback(window, this::cursorPosCallback);
        glfwSetMouseButtonCallback(window, this::mouseButtonCallback);
        glfwSetCharCallback(window, this::charCallback);
        glfwSetScrollCallback(window, this::scrollCallback);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        TextureAtlas.init();
        WorldSaveManager.loadOptions();
        LanguageManager.load(LanguageManager.getCurrentLang());

        sunTextureID = TextureLoader.loadTexture("/assets/textures/misc/sun.png");
        moonTextureID = TextureLoader.loadTexture("/assets/textures/misc/moon.png");
        for (int i = 0; i < 10; i++) {
            destroyStageTextureIDs[i] = TextureLoader.loadTexture("/assets/textures/block/destroy/destroy_stage_" + i + ".png");
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        float[] lightAmbient = {0.2f, 0.2f, 0.2f, 1.0f};
        float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
        float[] lightPosition = {0.0f, 10.0f, 10.0f, 1.0f};
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        glClearColor(0.5f, 0.7f, 1.0f, 0.0f);

        mainMenu = new MainMenu(this);
        FontRenderer.initFont();
        RecipeManager.loadRecipes();
        playMenu = new PlayMenu(this);
        optionsMenu = new OptionsMenu(this);
        keybindsMenu = new KeybindsMenu(this, optionsMenu);
        loadingScreen = new LoadingScreen(this);
        pauseMenu = new PauseMenu(this);
        currentState = GameState.MAIN_MENU;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (currentState == GameState.KEYBINDS_MENU) {
            keybindsMenu.handleKey(key, action);
            return;
        }

        if (currentState == GameState.IN_GAME && isItemPickerOpen) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                isItemPickerOpen = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
            return;
        }

        if (currentState == GameState.IN_GAME && isCommandConsoleOpen) {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                if (key == GLFW_KEY_ESCAPE) {
                    isCommandConsoleOpen = false;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                } else if (key == GLFW_KEY_BACKSPACE) {
                    if (commandInput.length() > 0) {
                        commandInput = commandInput.substring(0, commandInput.length() - 1);
                    }
                } else if (key == GLFW_KEY_ENTER && action == GLFW_PRESS) {
                    executeCommand(commandInput);
                    isCommandConsoleOpen = false;
                    glfwSetInputMode(window, GLFW_CURSOR, isItemPickerOpen ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_DISABLED);
                }
            }
            return;
        }

        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            if (currentState == GameState.IN_GAME) {
                pauseGame();
            } else if (currentState == GameState.PAUSE_MENU) {
                resumeGame();
            } else if (currentState == GameState.OPTIONS_MENU) {
                optionsMenu.goBack();
            } else if (currentState == GameState.KEYBINDS_MENU) {
                currentState = GameState.OPTIONS_MENU;
            } else if (currentState == GameState.PLAY_MENU) {
                playMenu.goBack();
            } else if (currentState == GameState.INVENTORY) {
                inventoryMenu.onClose();
                currentState = GameState.IN_GAME;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            } else if (currentState == GameState.CRAFTING_TABLE) {
                craftingTableMenu.onClose();
                currentState = GameState.IN_GAME;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            } else if (currentState == GameState.FURNACE) {
                furnaceMenu.onClose();
                currentState = GameState.IN_GAME;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }

        if (action == GLFW_PRESS) {
            if (currentState == GameState.IN_GAME) {
                if (COMMANDS_ENABLED && key == GLFW_KEY_SLASH) {
                    isCommandConsoleOpen = true;
                    commandInput = "";
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    return;
                }
                if (key == GLFW_KEY_F3) {
                    showDebug = !showDebug;
                }
                if (key == GLFW_KEY_F5) {
                    cameraMode = (cameraMode + 1) % 3;
                }
                if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                    int slotIndex = key - GLFW_KEY_1;
                    player.setSelectedSlot(slotIndex);
                }
                if (key == KEY_INVENTORY) {
                    currentState = GameState.INVENTORY;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
                if (key == KEY_DROP) {
                    player.dropSelectedStack();
                }
            } else if (currentState == GameState.INVENTORY) {
                if (key == KEY_INVENTORY) {
                    inventoryMenu.onClose();
                    currentState = GameState.IN_GAME;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
            } else if (currentState == GameState.CRAFTING_TABLE) {
                if (key == KEY_INVENTORY) {
                    craftingTableMenu.onClose();
                    currentState = GameState.IN_GAME;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
            } else if (currentState == GameState.FURNACE) {
                if (key == KEY_INVENTORY) {
                    furnaceMenu.onClose();
                    currentState = GameState.IN_GAME;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
            }
        }
    }

    private void cursorPosCallback(long window, double xpos, double ypos) {
        double dx = xpos - lastX;
        double dy = ypos - lastY;
        lastX = xpos;
        lastY = ypos;

        if (currentState == GameState.IN_GAME) {
            if (!isCommandConsoleOpen && !isItemPickerOpen) {
                player.addRotation((float) dx, (float) dy);
            }
        } else if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.PLAY_MENU) {
            playMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.OPTIONS_MENU) {
            optionsMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.KEYBINDS_MENU) {
            keybindsMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.PAUSE_MENU) {
            pauseMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.INVENTORY) {
            inventoryMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.CRAFTING_TABLE) {
            craftingTableMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.FURNACE) {
            furnaceMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.DEATH) {
            deathMenu.handleMouseMove(xpos, ypos);
        }
    }

    private void mouseButtonCallback(long window, int button, int action, int mods) {
        if (currentState == GameState.IN_GAME && isItemPickerOpen) {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                handleItemPickerClick(lastX, lastY);
            }
            return;
        }
        if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.PLAY_MENU) {
            playMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.OPTIONS_MENU) {
            optionsMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.KEYBINDS_MENU) {
            keybindsMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.PAUSE_MENU) {
            pauseMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.INVENTORY) {
            inventoryMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.CRAFTING_TABLE) {
            craftingTableMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.FURNACE) {
            furnaceMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.DEATH) {
            deathMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.IN_GAME) {
            int interactButton = SWAP_MOUSE_BUTTONS ? GLFW_MOUSE_BUTTON_LEFT : GLFW_MOUSE_BUTTON_RIGHT;
            int pushButton = SWAP_MOUSE_BUTTONS ? GLFW_MOUSE_BUTTON_RIGHT : GLFW_MOUSE_BUTTON_LEFT;
            if (button == interactButton && action == GLFW_PRESS) {
                SulfurCube hitCube = getTargetCube(4.5f);
                if (hitCube != null) {
                    interactWithCube(hitCube);
                    return;
                }
                Block target = player.getTargetBlock(world, 4.5f);
                if (target instanceof CraftingTable) {
                    currentState = GameState.CRAFTING_TABLE;
                    craftingTableMenu.setBlock((CraftingTable) target);
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                } else if (target instanceof Furnace) {
                    currentState = GameState.FURNACE;
                    furnaceMenu.setBlock((Furnace) target);
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            } else if (button == pushButton && action == GLFW_PRESS) {
                SulfurCube hitCube = getTargetCube(4.5f);
                if (hitCube != null) {
                    pushCube(hitCube);
                    return;
                }
            }
        }
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;
        final double PHYSICS_STEP = 1.0 / 60.0;

        while (!glfwWindowShouldClose(window)) {
            finishWorldInitialization();
            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            if (deltaTime > 0.1) deltaTime = 0.1;
            this.lastDeltaTime = (float) deltaTime;

            frameCount++;
            fpsTimer += deltaTime;
            if (fpsTimer >= 1.0) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer -= 1.0;
            }

            boolean isTickingState = (currentState == GameState.IN_GAME ||
                    currentState == GameState.INVENTORY ||
                    currentState == GameState.CRAFTING_TABLE ||
                    currentState == GameState.FURNACE);
            if (isTickingState) {
                timeOfDay += deltaTime * 20.0f;
                if (timeOfDay >= 24000.0f) {
                    timeOfDay -= 24000.0f;
                }
            }

            if (isTickingState) {
                if (oreVisionTimer > 0.0) {
                    oreVisionTimer -= deltaTime;
                    if (oreVisionTimer < 0.0) oreVisionTimer = 0.0;
                }
                accumulator += deltaTime;
                while (accumulator >= PHYSICS_STEP) {
                    if (currentState == GameState.IN_GAME) {
                        boolean blockPlayerInput = isCommandConsoleOpen || isItemPickerOpen;
                        player.handleInput(window, blockPlayerInput);
                        player.update(window, PHYSICS_STEP, blockPlayerInput);
                    }
                    world.updateDroppedItems((float) PHYSICS_STEP, player);
                    world.updateWater(PHYSICS_STEP);
                    world.updateFurnaces(PHYSICS_STEP);
                    world.updateSulfurCubes((float) PHYSICS_STEP, player);
                    accumulator -= PHYSICS_STEP;
                }
                if (player.isDead()) {
                    isCommandConsoleOpen = false;
                    isItemPickerOpen = false;
                    currentState = GameState.DEATH;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
            }

            updateSkyAndLighting();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (currentState == GameState.IN_GAME) {
                renderGame((float) (accumulator / PHYSICS_STEP));
            } else if (currentState == GameState.INVENTORY) {
                renderGame(1.0f);
                inventoryMenu.render(window);
            } else if (currentState == GameState.CRAFTING_TABLE) {
                renderGame(1.0f);
                craftingTableMenu.render(window);
            } else if (currentState == GameState.FURNACE) {
                renderGame(1.0f);
                furnaceMenu.render(window);
            } else if (currentState == GameState.DEATH) {
                renderGame(1.0f);
                deathMenu.render(window);
            } else if (currentState == GameState.PAUSE_MENU) {
                renderGame(1.0f);
                pauseMenu.render();
            } else if (currentState == GameState.OPTIONS_MENU) {
                if (optionsMenu.getPreviousState() == GameState.PAUSE_MENU) {
                    renderGame(1.0f);
                }
                optionsMenu.render();
            } else if (currentState == GameState.KEYBINDS_MENU) {
                keybindsMenu.render();
            } else if (currentState == GameState.PLAY_MENU) {
                playMenu.render();
            } else if (currentState == GameState.LOADING) {
                loadingScreen.render();
            } else {
                renderMainMenu();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderGame(float alpha) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float aspect = (float) width[0] / height[0];
        glViewport(0, 0, width[0], height[0]);
        float targetFov = (player != null && player.isSprinting()) ? 78.0f : 70.0f;
        float transitionFactor = Math.min(1.0f, 6.0f * lastDeltaTime);
        currentFov += (targetFov - currentFov) * transitionFactor;

        float farClip = Math.max(100.0f, World.RENDER_DISTANCE * 16.0f * 1.5f);
        perspective(currentFov, aspect, 0.1f, farClip);

        player.applyCameraTransform(alpha, cameraMode);

        float angle = (timeOfDay / 24000.0f) * 360.0f - 90.0f;
        float angleRad = (float) Math.toRadians(angle);
        float lx = (float) -Math.sin(angleRad);
        float ly = (float) Math.cos(angleRad);
        float lz = 0.0f;
        if (ly >= 0.0f) {
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{lx, ly, lz, 0.0f});
        } else {
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{-lx, -ly, -lz, 0.0f});
        }

        renderSkyEnvironment();
        if (GAMMA_FULL_BRIGHT) {
            glDisable(GL_LIGHTING);
        } else {
            glEnable(GL_LIGHTING);
        }
        world.render(player);
        if (oreVisionTimer > 0.0) {
            renderOreVisionOutline();
        }
        player.renderPlayerModel(alpha, cameraMode);

        renderBreakingBlockOverlay();

        if (hotbar != null) {
            hotbar.render(window);
        }

        renderCrosshair();

        renderDebugInfo();

        renderChatMessages();

        renderCommandConsole();
        renderItemPicker();

        if (cameraMode == 0 && currentState == GameState.IN_GAME && !isItemPickerOpen) {
            renderItemInHand(alpha);
        }
    }

    private void renderItemInHand(float alpha) {
        ItemStack held = player.getInventory()[player.getSelectedSlot()];

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        perspective(70, (float)w[0]/h[0], 0.01f, 10.0f);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glClear(GL_DEPTH_BUFFER_BIT);

        float swing = (float) Math.sin(player.getWalkTime() * 0.5f) * 0.05f;
        float bob = (float) Math.abs(Math.cos(player.getWalkTime() * 0.5f)) * 0.03f;

        if (held != null) {
            glTranslatef(0.6f + swing, -0.5f + bob, -0.8f);
            glRotatef(-15.0f, 0, 1, 0);
            glRotatef(10.0f, 1, 0, 0);
            glScalef(0.4f, 0.4f, 0.4f);

            GuiRenderer.draw3DBlockInHand(held.getType());
        } else {
            // Render hand
            glColor4f(1.0f, 0.8f, 0.6f, 1.0f); // Skin color
            glTranslatef(0.5f + swing, -0.6f + bob, -0.8f);
            glRotatef(-20.0f, 0, 1, 0);
            glScalef(0.2f, 0.6f, 0.2f);
            drawBox(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        }

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_LIGHTING);
    }

    private void drawBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        glBegin(GL_QUADS);
        glVertex3f(x1, y2, z1); glVertex3f(x1, y2, z2); glVertex3f(x2, y2, z2); glVertex3f(x2, y2, z1);
        glVertex3f(x1, y1, z1); glVertex3f(x2, y1, z1); glVertex3f(x2, y1, z2); glVertex3f(x1, y1, z2);
        glVertex3f(x1, y1, z1); glVertex3f(x2, y1, z1); glVertex3f(x2, y2, z1); glVertex3f(x1, y2, z1);
        glVertex3f(x1, y1, z2); glVertex3f(x1, y2, z2); glVertex3f(x2, y2, z2); glVertex3f(x2, y1, z2);
        glVertex3f(x2, y1, z1); glVertex3f(x2, y1, z2); glVertex3f(x2, y2, z2); glVertex3f(x2, y2, z1);
        glVertex3f(x1, y1, z1); glVertex3f(x1, y2, z1); glVertex3f(x1, y2, z2); glVertex3f(x1, y1, z2);
        glEnd();
    }

    private void renderCrosshair() {
        if (currentState != GameState.IN_GAME) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float cx = width[0] / 2.0f;
        float cy = height[0] / 2.0f;

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width[0], height[0], 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ZERO);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glLineWidth(2.0f);
        glBegin(GL_LINES);
        glVertex2f(cx - CROSSHAIR_SIZE, cy);
        glVertex2f(cx + CROSSHAIR_SIZE, cy);
        glVertex2f(cx, cy - CROSSHAIR_SIZE);
        glVertex2f(cx, cy + CROSSHAIR_SIZE);
        glEnd();

        glDisable(GL_BLEND);
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }


    private void renderBreakingBlockOverlay() {
        Block target = player.getTargetBlock(world, 4.5f);
        float progress = player.getBreakProgress();
        if (target == null) return;

        int bx = target.getX();
        int by = target.getY();
        int bz = target.getZ();
        float h = target.getBlockHeight();

        if (progress > 0.0f) {
            int stage = (int) (progress * 10.0f);
            if (stage > 9) stage = 9;
            if (stage < 0) stage = 0;

            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, destroyStageTextureIDs[stage]);

            glDisable(GL_LIGHTING);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ZERO, GL_SRC_COLOR);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glEnd();

            glDisable(GL_TEXTURE_2D);
        }

        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.0f, 0.0f, 0.0f, 0.4f);
        glLineWidth(2.0f);
        glBegin(GL_LINES);
        float minX = bx - 0.002f;
        float maxX = bx + 1.002f;
        float minY = by - 0.002f;
        float maxY = by + h + 0.002f;
        float minZ = bz - 0.002f;
        float maxZ = bz + 1.002f;

        glVertex3f(minX, minY, minZ); glVertex3f(maxX, minY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, minY, maxZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(minX, minY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, minY, minZ);

        glVertex3f(minX, maxY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(maxX, maxY, minZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(maxX, maxY, maxZ); glVertex3f(minX, maxY, maxZ);
        glVertex3f(minX, maxY, maxZ); glVertex3f(minX, maxY, minZ);

        glVertex3f(minX, minY, minZ); glVertex3f(minX, maxY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, maxY, maxZ);
        glEnd();
        glLineWidth(1.0f);

        glDisable(GL_BLEND);
        if (!GAMMA_FULL_BRIGHT) {
            glEnable(GL_LIGHTING);
        }
        glEnable(GL_TEXTURE_2D);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderMainMenu() {
        mainMenu.render();
    }

    public void setGameState(GameState state) {
        this.currentState = state;
        if (state == GameState.PLAY_MENU) {
            playMenu.refreshSlots();
        }
    }

    public void enterPlayMenu() {
        playMenu.refreshSlots();
        setGameState(GameState.PLAY_MENU);
    }

    public void enterOptionsMenu(GameState prev) {
        optionsMenu.setPreviousState(prev);
        setGameState(GameState.OPTIONS_MENU);
    }

    public void startNewOrLoadWorld(String worldName) {
        currentState = GameState.LOADING;
        worldInitializationPending = false;
        worldLoadFailure = null;
        loadingScreen.setProgress(0, "Initializing...");

        Thread loader = new Thread(() -> {
            try {
                World loadedWorld = new World();
                loadedWorld.setWorldName(worldName);
                Player loadedPlayer = new Player(loadedWorld);

                loadingScreen.setProgress(0.2f, "Loading level data...");
                if (WorldSaveManager.worldExists(worldName)) {
                    timeOfDay = WorldSaveManager.loadWorld(loadedWorld, loadedPlayer, worldName);
                } else {
                    loadedPlayer.respawn();
                    timeOfDay = 6000.0f;
                    WorldSaveManager.saveWorld(loadedWorld, loadedPlayer, timeOfDay, worldName);
                }

                loadingScreen.setProgress(0.4f, "Generating spawn area...");
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        loadedWorld.getOrLoadChunk(x, z, true);
                        float progress = 0.4f + ((x + 2) * 5 + (z + 2)) / 25.0f * 0.4f;
                        loadingScreen.setProgress(progress, "Generating chunk " + x + ", " + z);
                    }
                }

                world = loadedWorld;
                player = loadedPlayer;
            } catch (Exception e) {
                worldLoadFailure = e;
            } finally {
                worldInitializationPending = true;
            }
        }, "world-loader");
        loader.start();
    }

    private void finishWorldInitialization() {
        if (!worldInitializationPending) return;
        worldInitializationPending = false;

        if (worldLoadFailure != null) {
            worldLoadFailure.printStackTrace();
            worldLoadFailure = null;
            world = null;
            player = null;
            currentState = GameState.PLAY_MENU;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            return;
        }

        hotbar = new Hotbar(player);
        hotbar.init();
        inventoryMenu = new InventoryMenu(player);
        inventoryMenu.init();
        craftingTableMenu = new CraftingTableMenu(player);
        craftingTableMenu.init();
        furnaceMenu = new FurnaceMenu(player);
        furnaceMenu.init();
        deathMenu = new DeathMenu(this, player);
        deathMenu.init();
        loadingScreen.setProgress(1.0f, "Done!");
        resumeGame();
    }

    public void saveAndQuit() {
        if (world != null && player != null && world.getWorldName() != null) {
            WorldSaveManager.saveWorld(world, player, timeOfDay, world.getWorldName());
        }
        world = null;
        player = null;
        hotbar = null;
        inventoryMenu = null;
        craftingTableMenu = null;
        furnaceMenu = null;
        deathMenu = null;
        setGameState(GameState.MAIN_MENU);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    public void resumeGame() {
        currentState = GameState.IN_GAME;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetCursorPos(window, 1280 / 2.0, 720 / 2.0);
        lastX = 1280 / 2.0;
        lastY = 720 / 2.0;
    }

    public void pauseGame() {
        isCommandConsoleOpen = false;
        setGameState(GameState.PAUSE_MENU);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    public void quitGame() {
        glfwSetWindowShouldClose(window, true);
    }

    private void perspective(float fov, float aspect, float zNear, float zFar) {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fov / 2));
        float xmax = ymax * aspect;
        glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
    }

    public static void setWindowIcon(long window, String resourcePath) {
        try {
            InputStream stream = Game.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                System.err.println("Nie znaleziono zasobu: " + resourcePath);
                return;
            }
            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixelsRaw = new int[width * height];
            image.getRGB(0, 0, width, height, pixelsRaw, 0, width);
            ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixelsRaw[y * width + x];
                    pixels.put((byte) ((pixel >> 16) & 0xFF));
                    pixels.put((byte) ((pixel >> 8) & 0xFF));
                    pixels.put((byte) (pixel & 0xFF));
                    pixels.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            pixels.flip();
            GLFWImage.Buffer icon = GLFWImage.malloc(1);
            icon.width(width);
            icon.height(height);
            icon.pixels(pixels);
            glfwSetWindowIcon(window, icon);
            icon.free();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderDebugInfo() {
        if (!showDebug) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width[0], height[0], 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        String posText = "Block: " + px + " " + py + " " + pz;
        String fpsText = fps + " FPS";

        FontRenderer.drawStringRegular(posText, 10, 25);
        FontRenderer.drawStringRegular(fpsText, 10, 50);

        if (!GAMMA_FULL_BRIGHT) {
            glEnable(GL_LIGHTING);
        }
        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void updateSkyAndLighting() {
        if (player == null || world == null) return;

        float[] skyColor = getSkyColor(timeOfDay);
        glClearColor(skyColor[0], skyColor[1], skyColor[2], 0.0f);

        float angle = (timeOfDay / 24000.0f) * 360.0f - 90.0f;
        float angleRad = (float) Math.toRadians(angle);
        float lx = (float) -Math.sin(angleRad);
        float ly = (float) Math.cos(angleRad);
        float lz = 0.0f;

        float sunHeight = Math.max(0.0f, ly);
        float ambient = 0.05f + sunHeight * 0.35f;
        float diffuse = 0.15f + sunHeight * 0.65f;

        float lr, lg, lb;
        if (ly >= 0.0f) {
            lr = 1.0f - (1.0f - sunHeight) * 0.1f;
            lg = 0.95f - (1.0f - sunHeight) * 0.55f;
            lb = 0.8f - (1.0f - sunHeight) * 0.6f;
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{lx, ly, lz, 0.0f});
        } else {
            lr = 0.15f;
            lg = 0.2f;
            lb = 0.3f;
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{-lx, -ly, -lz, 0.0f});
        }

        float[] lightAmbient = {ambient, ambient, ambient, 1.0f};
        float[] lightDiffuse = {diffuse * lr, diffuse * lg, diffuse * lb, 1.0f};
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
    }

    private static float lerp(float start, float end, float f) {
        return start + f * (end - start);
    }

    private static float[] getSkyColor(float time) {
        float r, g, b;
        if (time >= 400 && time < 11600) {
            r = 0.5f; g = 0.7f; b = 1.0f;
        } else if (time >= 11600 && time < 12000) {
            float f = (time - 11600.0f) / 400.0f;
            r = lerp(0.5f, 0.9f, f);
            g = lerp(0.7f, 0.4f, f);
            b = lerp(1.0f, 0.2f, f);
        } else if (time >= 12000 && time < 12400) {
            float f = (time - 12000.0f) / 400.0f;
            r = lerp(0.9f, 0.02f, f);
            g = lerp(0.4f, 0.02f, f);
            b = lerp(0.2f, 0.05f, f);
        } else if (time >= 12400 && time < 23200) {
            r = 0.02f; g = 0.02f; b = 0.05f;
        } else if (time >= 23200 && time < 23600) {
            float f = (time - 23200.0f) / 400.0f;
            r = lerp(0.02f, 0.7f, f);
            g = lerp(0.02f, 0.4f, f);
            b = lerp(0.05f, 0.3f, f);
        } else if (time >= 23600 && time < 24000) {
            float f = (time - 23600.0f) / 400.0f;
            r = lerp(0.7f, 0.9f, f);
            g = lerp(0.4f, 0.6f, f);
            b = lerp(0.3f, 0.4f, f);
        } else {
            float f = time / 400.0f;
            r = lerp(0.9f, 0.5f, f);
            g = lerp(0.6f, 0.7f, f);
            b = lerp(0.4f, 1.0f, f);
        }
        return new float[]{r, g, b};
    }

    private void renderSkyEnvironment() {
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        glPushMatrix();
        glTranslatef(player.getX(), player.getY(), player.getZ());

        float skyAngle = (timeOfDay / 24000.0f) * 360.0f - 90.0f;
        glRotatef(skyAngle, 0.0f, 0.0f, 1.0f);

        glBindTexture(GL_TEXTURE_2D, sunTextureID);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBegin(GL_QUADS);
        float sunSize = 10.0f;
        float sunDist = 80.0f;
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-sunSize, sunDist, sunSize);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(sunSize, sunDist, sunSize);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(sunSize, sunDist, -sunSize);
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-sunSize, sunDist, -sunSize);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, moonTextureID);
        glBegin(GL_QUADS);
        float moonSize = 10.0f;
        float moonDist = -80.0f;
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-moonSize, moonDist, -moonSize);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(moonSize, moonDist, -moonSize);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(moonSize, moonDist, moonSize);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-moonSize, moonDist, moonSize);
        glEnd();

        glPopMatrix();

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        if (!GAMMA_FULL_BRIGHT) {
            glEnable(GL_LIGHTING);
        }
    }

    private void charCallback(long window, int codepoint) {
        if (currentState == GameState.IN_GAME && isCommandConsoleOpen) {
            if (commandInput.length() < 100) {
                commandInput += (char) codepoint;
            }
        }
    }

    private void executeCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) return;

        String cmd = commandLine.trim();
        if (!cmd.startsWith("/")) return;

        String[] parts = cmd.substring(1).split("\\s+");
        if (parts.length == 0) return;

        String commandName = parts[0].toLowerCase();
        switch (commandName) {
            case "tp":
                if (parts.length >= 4) {
                    try {
                        float tx = Float.parseFloat(parts[1]);
                        float ty = Float.parseFloat(parts[2]);
                        float tz = Float.parseFloat(parts[3]);

                        float maxCoord = 2147483500.0f;
                        if (tx > maxCoord) tx = maxCoord;
                        if (tx < -maxCoord) tx = -maxCoord;
                        if (ty > 128.0f) ty = 128.0f;
                        if (ty < 0.0f) ty = 0.0f;
                        if (tz > maxCoord) tz = maxCoord;
                        if (tz < -maxCoord) tz = -maxCoord;

                        player.setX(tx);
                        player.setY(ty);
                        player.setZ(tz);
                        player.setVelocityY(0);
                        player.resetPrevPosition();
                    } catch (NumberFormatException e) {
                        addChatMessage("Invalid coordinate format in /tp command!");
                    }
                }
                break;
            case "speed":
                if (parts.length >= 2) {
                    String speedArg = parts[1].toLowerCase();
                    if (speedArg.equals("default")) {
                        player.setSpeed(0.07f);
                    } else {
                        try {
                            float val = Float.parseFloat(speedArg);
                            if (val < 0.01f) val = 0.01f;
                            if (val > 5.0f) val = 5.0f;
                            player.setSpeed(val);
                        } catch (NumberFormatException e) {
                            addChatMessage("Invalid speed value!");
                        }
                    }
                }
                break;
            case "god":
                if (parts.length >= 2) {
                    String arg = parts[1].toLowerCase();
                    if (arg.equals("on") || arg.equals("true") || arg.equals("1")) {
                        player.setGodMode(true);
                    } else if (arg.equals("off") || arg.equals("false") || arg.equals("0")) {
                        player.setGodMode(false);
                    } else {
                        addChatMessage("Usage: /god [on|off]");
                        break;
                    }
                } else {
                    player.setGodMode(!player.isGodMode());
                }
                if (player.isGodMode()) {
                    addChatMessage("God mode enabled!");
                } else {
                    addChatMessage("God mode disabled!");
                }
                break;
            case "gamma":
                if (parts.length >= 2) {
                    String arg = parts[1].toLowerCase();
                    if (arg.equals("on") || arg.equals("true") || arg.equals("1")) {
                        GAMMA_FULL_BRIGHT = true;
                    } else if (arg.equals("off") || arg.equals("false") || arg.equals("0")) {
                        GAMMA_FULL_BRIGHT = false;
                    } else {
                        addChatMessage("Usage: /gamma [on|off]");
                        break;
                    }
                } else {
                    GAMMA_FULL_BRIGHT = !GAMMA_FULL_BRIGHT;
                }
                if (world != null) {
                    for (Chunk c : world.getChunks().values()) {
                        c.markDirty();
                    }
                }
                if (GAMMA_FULL_BRIGHT) {
                    addChatMessage("Gamma enabled (full bright)!");
                } else {
                    addChatMessage("Gamma disabled!");
                }
                break;
            case "time":
                if (parts.length >= 2) {
                    String timeArg = parts[1].toLowerCase();
                    if (timeArg.equals("day")) {
                        timeOfDay = 6000.0f;
                        addChatMessage("Set time to day (6000)");
                    } else if (timeArg.equals("night")) {
                        timeOfDay = 18000.0f;
                        addChatMessage("Set time to night (18000)");
                    } else {
                        try {
                            float val = Float.parseFloat(timeArg);
                            if (val < 0.0f) val = 0.0f;
                            if (val >= 24000.0f) val = val % 24000.0f;
                            timeOfDay = val;
                            addChatMessage("Set time to " + (int)val);
                        } catch (NumberFormatException e) {
                            addChatMessage("Usage: /time [day|night|<number>]");
                        }
                    }
                } else {
                    addChatMessage("Usage: /time [day|night|<number>]");
                }
                break;
            case "orevision":
                oreVisionTimer = 10.0;
                addChatMessage("Ore vision activated for 10 seconds!");
                break;
            case "summon":
                if (parts.length >= 2) {
                    String entityName = parts[1].toLowerCase();
                    if (entityName.equals("sulfur_cube")) {
                        world.spawnSulfurCube(player.getX(), player.getY(), player.getZ());
                        addChatMessage("Summoned sulfur_cube at player's position.");
                    } else {
                        addChatMessage("Unknown entity: " + entityName);
                    }
                } else {
                    addChatMessage("Usage: /summon <entity>");
                }
                break;
            case "itempick":
                itemPickerAmount = 1;
                isItemPickerOpen = true;
                break;
            case "give":
                if (parts.length >= 2) {
                    String inputName = parts[1].toLowerCase();
                    String typeName = parts[1].toUpperCase();

                    if (inputName.equals("oak_log")) typeName = "LOG";
                    if (inputName.equals("oak_planks")) typeName = "PLANKS";
                    if (inputName.equals("maslo")) typeName = "MASLO_INGOT";
                    if (inputName.equals("ruda_masla")) typeName = "MASLO_ORE";
                    if (inputName.equals("kilof_maslo") || inputName.equals("kilof_maslany")) typeName = "MASLO_PICKAXE";

                    try {
                        int amount = 1;
                        if (parts.length >= 3) {
                            amount = Integer.parseInt(parts[2]);
                        }
                        ItemType type = ItemType.valueOf(typeName);
                        for (int i = 0; i < amount; i++) {
                            player.addItem(type);
                        }
                        addChatMessage("Gave " + amount + " of " + typeName);
                    } catch (NumberFormatException e) {
                        addChatMessage("Invalid amount!");
                    } catch (IllegalArgumentException e) {
                        addChatMessage("Unknown item type: " + typeName);
                    }
                } else {
                    addChatMessage("Usage: /give <ITEM_TYPE> [amount]");
                }
                break;
            default:
                addChatMessage("Unknown command: " + commandName);
                break;
        }
    }

    private void renderCommandConsole() {
        if (!isCommandConsoleOpen) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float currentW = width[0];
        float currentH = height[0];

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float barY = currentH - 35;
        float barHeight = 24;
        float padding = 8;

        glColor4f(0.0f, 0.0f, 0.0f, 0.6f);
        glBegin(GL_QUADS);
        glVertex2f(padding, barY);
        glVertex2f(currentW - padding, barY);
        glVertex2f(currentW - padding, barY + barHeight);
        glVertex2f(padding, barY + barHeight);
        glEnd();

        glDisable(GL_BLEND);

        List<String> suggestions = getCommandSuggestions();
        if (!suggestions.isEmpty()) {
            float suggestionLineHeight = 16.0f;
            int suggestionColumns = suggestions.size() > 12 ? 2 : 1;
            int suggestionRows = (suggestions.size() + suggestionColumns - 1) / suggestionColumns;
            float panelHeight = suggestionRows * suggestionLineHeight + 8.0f;
            float panelY = barY - panelHeight - 4.0f;

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0.0f, 0.0f, 0.0f, 0.72f);
            glBegin(GL_QUADS);
            glVertex2f(padding, panelY);
            glVertex2f(Math.min(currentW - padding, 520.0f), panelY);
            glVertex2f(Math.min(currentW - padding, 520.0f), barY - 4.0f);
            glVertex2f(padding, barY - 4.0f);
            glEnd();
            glDisable(GL_BLEND);

            for (int i = 0; i < suggestions.size(); i++) {
                int column = i / suggestionRows;
                int row = i % suggestionRows;
                glColor4f(0.85f, 0.9f, 1.0f, 1.0f);
                FontRenderer.drawString(suggestions.get(i), padding + 6 + column * 250.0f,
                        panelY + 14.0f + row * suggestionLineHeight);
            }
        }

        String cursor = (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
        String displayText = commandInput + cursor;

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        FontRenderer.drawString(displayText, padding + 6, barY + 17);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glEnable(GL_DEPTH_TEST);
    }

    private void renderItemPicker() {
        if (!isItemPickerOpen || currentState != GameState.IN_GAME) return;

        int[] framebufferWidth = new int[1];
        int[] framebufferHeight = new int[1];
        glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
        float currentW = framebufferWidth[0];
        float currentH = framebufferHeight[0];

        ItemType[] itemTypes = getItemPickerItems(itemPickerCategory);
        int columns = 8;
        int rows = (itemTypes.length + columns - 1) / columns;
        float cellSize = 68.0f;
        float panelWidth = columns * cellSize + 36.0f;
        float panelHeight = 98.0f + rows * cellSize + 36.0f;
        float panelX = (currentW - panelWidth) * 0.5f;
        float panelY = (currentH - panelHeight) * 0.5f;
        float gridX = panelX + 18.0f;
        float gridY = panelY + 98.0f;
        int hoveredIndex = getItemPickerIndexAt(lastX, lastY);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawItemPickerRect(0, 0, currentW, currentH, 0.0f, 0.0f, 0.0f, 0.65f);
        drawItemPickerRect(panelX, panelY, panelWidth, panelHeight, 0.12f, 0.13f, 0.16f, 0.98f);

        float tabY = panelY + 62.0f;
        float tabWidth = (panelWidth - 36.0f) / 3.0f;
        for (int tab = 0; tab < 3; tab++) {
            float tabX = panelX + 18.0f + tab * tabWidth;
            if (tab == itemPickerCategory) {
                drawItemPickerRect(tabX, tabY, tabWidth - 4.0f, 27.0f, 0.34f, 0.42f, 0.54f, 1.0f);
            } else {
                drawItemPickerRect(tabX, tabY, tabWidth - 4.0f, 27.0f, 0.20f, 0.22f, 0.26f, 1.0f);
            }
        }

        for (int i = 0; i < itemTypes.length; i++) {
            int column = i % columns;
            int row = i / columns;
            float slotX = gridX + column * cellSize;
            float slotY = gridY + row * cellSize;
            if (i == hoveredIndex) {
                drawItemPickerRect(slotX, slotY, 64.0f, 64.0f, 0.32f, 0.38f, 0.48f, 1.0f);
            } else {
                drawItemPickerRect(slotX, slotY, 64.0f, 64.0f, 0.20f, 0.22f, 0.26f, 1.0f);
            }
        }
        glDisable(GL_BLEND);

        for (int i = 0; i < itemTypes.length; i++) {
            int column = i % columns;
            int row = i / columns;
            float slotX = gridX + column * cellSize;
            float slotY = gridY + row * cellSize;
            GuiRenderer.draw3DBlock(slotX + 32.0f, slotY + 30.0f, 52.0f,
                    itemTypes[i], currentW, currentH);
        }

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        FontRenderer.drawStringRegular(LanguageManager.get("itempicker.title"), panelX + 18.0f, panelY + 25.0f);
        String amountLabel = LanguageManager.get("itempicker.amount") + ": " + itemPickerAmount;
        FontRenderer.drawStringRegular(amountLabel,
                panelX + panelWidth - 18.0f - FontRenderer.getStringWidth(amountLabel), panelY + 25.0f);
        if (hoveredIndex >= 0 && hoveredIndex < itemTypes.length) {
            FontRenderer.drawStringRegular(LanguageManager.getItemName(itemTypes[hoveredIndex]),
                    panelX + 18.0f, panelY + 49.0f);
        }
        String[] categoryKeys = {"itempicker.category.tools", "itempicker.category.items", "itempicker.category.blocks"};
        for (int tab = 0; tab < categoryKeys.length; tab++) {
            float tabX = panelX + 18.0f + tab * tabWidth;
            String label = LanguageManager.get(categoryKeys[tab]);
            float textX = tabX + (tabWidth - 4.0f - FontRenderer.getStringWidth(label)) / 2.0f;
            FontRenderer.drawStringRegular(label, textX, tabY + 18.0f);
        }
        FontRenderer.drawStringRegular(LanguageManager.get("itempicker.hint"),
                panelX + 18.0f, panelY + panelHeight - 12.0f);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        if (!GAMMA_FULL_BRIGHT) glEnable(GL_LIGHTING);
    }

    private ItemType[] getItemPickerItems(int category) {
        switch (category) {
            case 0:
                return new ItemType[]{ItemType.WOODEN_PICKAXE, ItemType.STONE_PICKAXE, ItemType.MASLO_PICKAXE};
            case 1:
                return new ItemType[]{ItemType.STICK, ItemType.KEBAB, ItemType.MASLO_INGOT,
                        ItemType.CLAY_BALL, ItemType.BRICK, ItemType.GREEN_DYE};
            default:
                return new ItemType[]{ItemType.DIRT, ItemType.GRASS_BLOCK, ItemType.STONE, ItemType.COBBLESTONE,
                        ItemType.SAND, ItemType.SANDSTONE, ItemType.CLAY, ItemType.TERRACOTTA,
                        ItemType.GREEN_TERRACOTTA, ItemType.BRICKS, ItemType.LOG, ItemType.PLANKS,
                        ItemType.LEAVES, ItemType.CACTUS, ItemType.GLASS, ItemType.WATER,
                        ItemType.FLOWING_WATER, ItemType.BEDROCK, ItemType.CRAFTING_TABLE, ItemType.FURNACE,
                        ItemType.KEBAB_ORE, ItemType.KEBAB_BLOCK, ItemType.MASLO_ORE};
        }
    }

    private void drawItemPickerRect(float x, float y, float width, float height,
                                    float red, float green, float blue, float alpha) {
        glColor4f(red, green, blue, alpha);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
    }

    private float[] getItemPickerMousePosition(double mouseX, double mouseY) {
        int[] framebufferWidth = new int[1];
        int[] framebufferHeight = new int[1];
        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
        glfwGetWindowSize(window, windowWidth, windowHeight);
        if (windowWidth[0] <= 0 || windowHeight[0] <= 0) return null;
        return new float[]{
                (float) (mouseX * framebufferWidth[0] / windowWidth[0]),
                (float) (mouseY * framebufferHeight[0] / windowHeight[0]),
                framebufferWidth[0], framebufferHeight[0]
        };
    }

    private int getItemPickerCategoryAt(double mouseX, double mouseY) {
        float[] pos = getItemPickerMousePosition(mouseX, mouseY);
        if (pos == null) return -1;
        float panelWidth = 8 * 68.0f + 36.0f;
        int rows = (getItemPickerItems(itemPickerCategory).length + 7) / 8;
        float panelHeight = 98.0f + rows * 68.0f + 36.0f;
        float panelX = (pos[2] - panelWidth) * 0.5f;
        float panelY = (pos[3] - panelHeight) * 0.5f;
        float tabY = panelY + 62.0f;
        float tabWidth = (panelWidth - 36.0f) / 3.0f;
        if (pos[1] < tabY || pos[1] >= tabY + 27.0f) return -1;
        for (int tab = 0; tab < 3; tab++) {
            float tabX = panelX + 18.0f + tab * tabWidth;
            if (pos[0] >= tabX && pos[0] < tabX + tabWidth - 4.0f) return tab;
        }
        return -1;
    }

    private int getItemPickerIndexAt(double mouseX, double mouseY) {
        float[] pos = getItemPickerMousePosition(mouseX, mouseY);
        if (pos == null) return -1;
        ItemType[] itemTypes = getItemPickerItems(itemPickerCategory);
        int columns = 8;
        int rows = (itemTypes.length + columns - 1) / columns;
        float cellSize = 68.0f;
        float panelWidth = columns * cellSize + 36.0f;
        float panelHeight = 98.0f + rows * cellSize + 36.0f;
        float panelX = (pos[2] - panelWidth) * 0.5f;
        float panelY = (pos[3] - panelHeight) * 0.5f;
        float gridX = panelX + 18.0f;
        float gridY = panelY + 98.0f;

        float relativeX = pos[0] - gridX;
        float relativeY = pos[1] - gridY;
        if (relativeX < 0.0f || relativeY < 0.0f) return -1;
        int column = (int) (relativeX / cellSize);
        int row = (int) (relativeY / cellSize);
        if (column >= columns || row >= rows) return -1;
        if (relativeX % cellSize >= 64.0f || relativeY % cellSize >= 64.0f) return -1;

        int index = row * columns + column;
        return index < itemTypes.length ? index : -1;
    }

    private void handleItemPickerClick(double mouseX, double mouseY) {
        int selectedCategory = getItemPickerCategoryAt(mouseX, mouseY);
        if (selectedCategory >= 0) {
            itemPickerCategory = selectedCategory;
            return;
        }

        ItemType[] itemTypes = getItemPickerItems(itemPickerCategory);
        int index = getItemPickerIndexAt(mouseX, mouseY);
        if (index < 0 || index >= itemTypes.length) return;

        ItemType selected = itemTypes[index];
        for (int i = 0; i < itemPickerAmount; i++) {
            player.addItem(selected);
        }
        addChatMessage(LanguageManager.get("itempicker.added") + " " + itemPickerAmount + "x "
                + LanguageManager.getItemName(selected));
    }

    private List<String> getCommandSuggestions() {
        List<String> options = new ArrayList<>();
        if (commandInput.isEmpty() || !commandInput.startsWith("/")) return options;

        String commandText = commandInput.substring(1);
        boolean trailingSpace = commandText.matches("(?s).*\\s$");
        String trimmed = commandText.trim();
        if (trimmed.isEmpty()) {
            options.addAll(List.of("/tp", "/speed", "/god", "/gamma", "/time", "/orevision", "/summon", "/give", "/itempick"));
            return options;
        }

        String[] tokens = trimmed.split("\\s+");
        String command = tokens[0].toLowerCase();
        if (tokens.length == 1 && !trailingSpace) {
            for (String name : List.of("tp", "speed", "god", "gamma", "time", "orevision", "summon", "give", "itempick")) {
                if (name.startsWith(command) && !name.equals(command)) options.add("/" + name);
            }
            if (!options.isEmpty()) return options;
        }

        int argumentIndex = tokens.length == 1 ? 1 : (trailingSpace ? tokens.length : tokens.length - 1);
        String prefix = tokens.length == 1 || trailingSpace ? "" : tokens[tokens.length - 1].toLowerCase();
        List<String> candidates = new ArrayList<>();
        switch (command) {
            case "give":
                if (argumentIndex == 1) {
                    for (ItemType type : ItemType.values()) candidates.add(getGiveItemName(type));
                } else if (argumentIndex == 2) {
                    candidates.add("<value>");
                }
                break;
            case "tp":
                if (argumentIndex >= 1 && argumentIndex <= 3) candidates.add("<value>");
                break;
            case "speed":
                if (argumentIndex == 1) {
                    candidates.add("default");
                    candidates.add("<value>");
                }
                break;
            case "god":
            case "gamma":
                if (argumentIndex == 1) candidates.addAll(List.of("on", "off"));
                break;
            case "time":
                if (argumentIndex == 1) candidates.addAll(List.of("day", "night", "<value>"));
                break;
            case "summon":
                if (argumentIndex == 1) candidates.add("sulfur_cube");
                break;
            default:
                break;
        }

        for (String candidate : candidates) {
            if (candidate.startsWith("<") || candidate.toLowerCase().startsWith(prefix)) {
                options.add(candidate);
                if (options.size() == 40) break;
            }
        }
        return options;
    }

    private String getGiveItemName(ItemType type) {
        switch (type) {
            case LOG: return "oak_log";
            case PLANKS: return "oak_planks";
            case SAND: return "sand";
            case SANDSTONE: return "sandstone";
            case CACTUS: return "cactus";
            case GREEN_DYE: return "green_dye";
            case MASLO_INGOT: return "maslo";
            case MASLO_ORE: return "ruda_masla";
            case MASLO_PICKAXE: return "kilof_maslo";
            default: return type.name().toLowerCase();
        }
    }

    private void scrollCallback(long window, double xoffset, double yoffset) {
        if (currentState == GameState.IN_GAME && isItemPickerOpen) {
            if (yoffset != 0.0) {
                int steps = Math.max(1, (int) Math.round(Math.abs(yoffset)));
                int direction = yoffset > 0.0 ? 1 : -1;
                itemPickerAmount = Math.max(1, Math.min(ItemStack.MAX_STACK_SIZE,
                        itemPickerAmount + direction * steps));
            }
        } else if (currentState == GameState.IN_GAME && !isCommandConsoleOpen) {
            int currentSlot = player.getSelectedSlot();
            if (yoffset < 0) {
                currentSlot = (currentSlot + 1) % 9;
            } else if (yoffset > 0) {
                currentSlot = (currentSlot - 1 + 9) % 9;
            }
            player.setSelectedSlot(currentSlot);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public long getWindowHandle() {
        return window;
    }

    private static class ChatMessage {
        String text;
        long timestamp;

        ChatMessage(String text) {
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final java.util.List<ChatMessage> chatMessages = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addChatMessage(String message) {
        chatMessages.add(new ChatMessage(message));
        if (chatMessages.size() > 50) {
            chatMessages.remove(0);
        }
    }

    private void renderChatMessages() {
        if (chatMessages.isEmpty()) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float currentW = width[0];
        float currentH = height[0];

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        long now = System.currentTimeMillis();
        int maxVisible = 10;
        int startIndex = Math.max(0, chatMessages.size() - maxVisible);
        java.util.List<ChatMessage> visible = chatMessages.subList(startIndex, chatMessages.size());

        float startY = currentH - (isCommandConsoleOpen ? 60 : 35);
        float lineHeight = 20;

        for (int i = 0; i < visible.size(); i++) {
            ChatMessage msg = visible.get(i);
            long age = now - msg.timestamp;

            float alpha = 1.0f;
            if (!isCommandConsoleOpen) {
                if (age > 10000) continue;
                if (age > 8000) {
                    alpha = 1.0f - (age - 8000) / 2000.0f;
                }
            }

            float y = startY - (visible.size() - 1 - i) * lineHeight;

            float textWidth = FontRenderer.getStringWidth(msg.text);
            glColor4f(0.0f, 0.0f, 0.0f, 0.4f * alpha);
            glBegin(GL_QUADS);
            glVertex2f(8, y - 14);
            glVertex2f(8 + textWidth + 6, y - 14);
            glVertex2f(8 + textWidth + 6, y + 4);
            glVertex2f(8, y + 4);
            glEnd();

            if (msg.text.startsWith("Unknown command") || msg.text.startsWith("Invalid")) {
                glColor4f(1.0f, 0.3f, 0.3f, alpha);
            } else {
                glColor4f(1.0f, 1.0f, 1.0f, alpha);
            }
            FontRenderer.drawString(msg.text, 11, y);
        }

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glEnable(GL_DEPTH_TEST);
    }

    private void renderOreVisionOutline() {
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glLineWidth(2.0f);

        int playerChunkX = (int) Math.floor(player.getX() / Chunk.CHUNK_SIZE_X);
        int playerChunkZ = (int) Math.floor(player.getZ() / Chunk.CHUNK_SIZE_Z);
        int radius = 4;

        glBegin(GL_LINES);
        for (int x = playerChunkX - radius; x <= playerChunkX + radius; x++) {
            for (int z = playerChunkZ - radius; z <= playerChunkZ + radius; z++) {
                Chunk chunk = world.getChunks().get(x + "_" + z);
                if (chunk != null && chunk.isGenerated()) {
                    int startX = chunk.getWorldX() * Chunk.CHUNK_SIZE_X;
                    int startZ = chunk.getWorldZ() * Chunk.CHUNK_SIZE_Z;
                    for (int cx = 0; cx < Chunk.CHUNK_SIZE_X; cx++) {
                        for (int cz = 0; cz < Chunk.CHUNK_SIZE_Z; cz++) {
                            for (int cy = 0; cy < 64; cy++) {
                                Block b = chunk.getBlock(cx, cy, cz);
                                if (b instanceof KebabOre) {
                                    glColor4f(0.0f, 1.0f, 0.0f, 0.8f);
                                    float bx = startX + cx;
                                    float by = cy;
                                    float bz = startZ + cz;
                                    drawWireframeBox(bx, by, bz);
                                } else if (b instanceof MasloOre) {
                                    glColor4f(1.0f, 1.0f, 0.0f, 0.8f);
                                    float bx = startX + cx;
                                    float by = cy;
                                    float bz = startZ + cz;
                                    drawWireframeBox(bx, by, bz);
                                }
                            }
                        }
                    }
                }
            }
        }
        glEnd();

        glLineWidth(1.0f);
        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        if (!GAMMA_FULL_BRIGHT) {
            glEnable(GL_LIGHTING);
        }
    }

    private void drawWireframeBox(float bx, float by, float bz) {
        float minX = bx - 0.002f;
        float maxX = bx + 1.002f;
        float minY = by - 0.002f;
        float maxY = by + 1.002f;
        float minZ = bz - 0.002f;
        float maxZ = bz + 1.002f;

        glVertex3f(minX, minY, minZ); glVertex3f(maxX, minY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, minY, maxZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(minX, minY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, minY, minZ);

        glVertex3f(minX, maxY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(maxX, maxY, minZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(maxX, maxY, maxZ); glVertex3f(minX, maxY, maxZ);
        glVertex3f(minX, maxY, maxZ); glVertex3f(minX, maxY, minZ);

        glVertex3f(minX, minY, minZ); glVertex3f(minX, maxY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, maxY, maxZ);
    }

    private SulfurCube getTargetCube(float maxDistance) {
        if (world == null || player == null) return null;
        float eyeX = player.getX();
        float eyeY = player.getY() + 1.7f;
        float eyeZ = player.getZ();
        float radYaw = (float) Math.toRadians(player.getYaw());
        float radPitch = (float) Math.toRadians(player.getPitch());
        float dirX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float) (Math.sin(radPitch));
        float dirZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));
        SulfurCube bestCube = null;
        float bestDist = maxDistance;
        for (SulfurCube cube : world.getSulfurCubes()) {
            float cx = cube.getX();
            float cy = cube.getY() + cube.getSize() / 2.0f;
            float cz = cube.getZ();
            float dx = cx - eyeX;
            float dy = cy - eyeY;
            float dz = cz - eyeZ;
            float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < bestDist) {
                float dot = (dx*dirX + dy*dirY + dz*dirZ) / dist;
                if (dot > 0.95f) {
                    bestCube = cube;
                    bestDist = dist;
                }
            }
        }
        return bestCube;
    }

    private void interactWithCube(SulfurCube cube) {
        ItemStack[] inventory = player.getInventory();
        int selectedSlot = player.getSelectedSlot();
        ItemStack held = inventory[selectedSlot];
        if (cube.getAbsorbedBlock() == null) {
            if (held != null) {
                ItemType type = held.getType();
                if (type == ItemType.LOG || type == ItemType.PLANKS || type == ItemType.COBBLESTONE) {
                    cube.setAbsorbedBlock(type);
                    held.setCount(held.getCount() - 1);
                    if (held.getCount() <= 0) inventory[selectedSlot] = null;
                }
            }
        } else {
            if (held == null) {
                ItemType type = cube.getAbsorbedBlock();
                player.addItem(type);
                cube.setAbsorbedBlock(null);
            }
        }
    }

    private void pushCube(SulfurCube cube) {
        float radYaw = (float) Math.toRadians(player.getYaw());
        float radPitch = (float) Math.toRadians(player.getPitch());
        float dirX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float) (Math.sin(radPitch));
        float dirZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));
        float pushForce = 3.5f;
        cube.setVelocities(dirX * pushForce, dirY * pushForce + 2.0f, dirZ * pushForce);
    }
}
