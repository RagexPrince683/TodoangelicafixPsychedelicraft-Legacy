/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraftcore;

import ivorius.pscoreutils.events.*;
import ivorius.psychedelicraft.events.PSCoreHandlerClient;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;

/**
 * Created by lukas on 21.02.14.
 */
public class PsycheCoreBusClient
{
    public static void preWorldRender(float partialTicks)
    {
        setPlayerAngles(partialTicks); // TODO Fix to allow cancellation later
        PsycheCoreBusCommon.EVENT_BUS.post(new RenderWorldEvent.Pre(partialTicks));
    }

    public static void postWorldRender(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new RenderWorldEvent.Post(partialTicks));
    }

    public static void psycheGLEnable(int cap)
    {
        PSCoreHandlerClient.onGLSwitch(cap, true);
    }

    public static void psycheGLDisable(int cap)
    {
        PSCoreHandlerClient.onGLSwitch(cap, false);
    }

    public static void psycheGLBlendFunc(int sFactor, int dFactor, int sfactorAlpha, int dfactorAlpha)
    {
        PSCoreHandlerClient.onGLBlendFunc(sFactor, dFactor, sfactorAlpha, dfactorAlpha);
    }

    public static void psycheGLActiveTexture(int texture)
    {
        PSCoreHandlerClient.onGLActiveTexture(texture);
    }

    public static void psycheGLFogi(int pname, int param)
    {
        PSCoreHandlerClient.onGLFogi(pname, param);
    }

    public static void psycheGLTranslatef(float x, float y, float z)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLTranslateEvent(x, y, z));
    }

    public static void psycheGLRotatef(float angle, float x, float y, float z)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLRotateEvent(angle, x, y, z));
    }

    public static void psycheGLScalef(float x, float y, float z)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLScaleEvent(x, y, z));
    }

    public static int psycheGLClear(int mask)
    {
        return PSCoreHandlerClient.onGLClear(mask);
    }

    /** Compatibility dispatch for integrations which consume the public Forge events. */
    public static void postGLSwitchEvent(int cap, boolean enabled)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLSwitchEvent(cap, enabled));
    }

    public static void postGLBlendFuncEvent(int source, int destination, int sourceAlpha, int destinationAlpha)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLBlendFuncEvent(source, destination, sourceAlpha, destinationAlpha));
    }

    public static void postGLActiveTextureEvent(int texture)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLActiveTextureEvent(texture));
    }

    public static void postGLFogiEvent(int parameter, int value)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLFogiEvent(parameter, value));
    }

    public static int postGLClearEvent(int mask)
    {
        GLClearEvent event = new GLClearEvent(mask);
        PsycheCoreBusCommon.EVENT_BUS.post(event);
        return event.currentMask;
    }

    public static void enableStandardItemLighting()
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new ItemLightingEvent(true));
    }

    public static void disableStandardItemLighting()
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new ItemLightingEvent(false));
    }

    public static void orientCamera(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new OrientCameraEvent(partialTicks));
    }

    public static void renderHeldItem(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new RenderHeldItemEvent(partialTicks));
    }

    public static void renderEntities(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new RenderEntitiesEvent(partialTicks));
    }

    public static boolean preRenderHand(float partialTicks)
    {
        return PsycheCoreBusCommon.EVENT_BUS.post(new RenderHandEvent.Pre(partialTicks));
    }

    public static void postRenderHand(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new RenderHandEvent.Pre(partialTicks));
    }

    public static void setPlayerAngles(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new SetPlayerAnglesEvent(partialTicks));
    }

    public static float getSoundVolume(float volume, ISound sound, SoundPoolEntry entry, SoundCategory category, SoundManager manager)
    {
        GetSoundVolumeEvent event = new GetSoundVolumeEvent(volume, sound, entry, category, manager);
        PsycheCoreBusCommon.EVENT_BUS.post(event);

        return event.volume;
    }

    public static boolean setupCameraTransform(float partialTicks)
    {
        return PsycheCoreBusCommon.EVENT_BUS.post(new SetupCameraTransformEvent(partialTicks));
    }

    public static boolean renderBlockOverlay(float partialTicks)
    {
        return PsycheCoreBusCommon.EVENT_BUS.post(new RenderBlockOverlayEvent(partialTicks));
    }

    public static void fixGLState()
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new GLStateFixEvent());
    }

    public static void enableLightmap()
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new LightmapSwitchEvent(true));
    }

    public static void disableLightmap()
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new LightmapSwitchEvent(false));
    }

    public static void preRenderSky(float partialTicks)
    {
        PsycheCoreBusCommon.EVENT_BUS.post(new RenderSkyEvent.Pre(partialTicks));
    }
}
