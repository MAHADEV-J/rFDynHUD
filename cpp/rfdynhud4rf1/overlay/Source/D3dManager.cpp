#include "extended_ISI_API.h"
#include <Windows.h>
#include "window_handle.h"
#include "handshake.hpp"
#include "direct_input.h"
#include "filesystem.h"
#include "common.h"
#include "util.h"
#include "logging.h"

static const char* RFACTOR_PATH = getRFactorPath();
static const char* PLUGIN_PATH = getPluginPath();
static const char* CONFIG_PATH = getConfigPath();

static char* fileBuffer = (char*)malloc( MAX_PATH );

Handshake* handshake = NULL;

OverlayTextureManager* textureManager = NULL;

unsigned short resX, resY;
bool window_activated = true;

static PixelBufferCallback* pixBuffCallback = new PixelBufferCallback();

HWND m_windowHandle = 0;

HWND getWindowHandle()
{
    return ( m_windowHandle );
}

unsigned char* PixelBufferCallback::getPixelBuffer( const unsigned char textureIndex, void** userObject )
{
    return ( handshake->jvmConn.d3dFuncs.getPixelData( textureIndex ) );
}

void PixelBufferCallback::releasePixelBuffer( const unsigned char textureIndex, unsigned char* buffer, void* userObject )
{
    handshake->jvmConn.d3dFuncs.releasePixelData( textureIndex, buffer );
}

void _onTextureRequested()
{
    if ( ( handshake != NULL ) && ( handshake->state == HANDSHAKE_STATE_COMPLETE ) )
    {
        logg( "Texture requested. Updating textures..." );
        
        handshake->jvmConn.d3dFuncs.updateAllTextureInfos();
        if ( textureManager->setupTextures( handshake->jvmConn.d3dFuncs.getNumTextures(), handshake->jvmConn.d3dFuncs.textureSizes, handshake->jvmConn.d3dFuncs.numUsedRectangles, handshake->jvmConn.d3dFuncs.usedRectangles ) )
            logg( "Textures successfully updated." );
        else
            logg( "Textures update failed." );
    }
}

void _onRealtimeEntered()
{
    //logg( "Entered realtime mode" );
}

void _onRealtimeExited()
{
    //logg( "Exited realtime mode" );
}

void _checkRenderModeResult( const char* source, const int result )
{
    //logg2( "Checking result from ", source, false );
    //loggi( ": ", result, true );
    
    if ( result == 0 )
    {
        handshake->isInRenderMode = false;
    }
    else if ( result == 1 )
    {
        handshake->isInRenderMode = true;
    }
    else if ( result == 2 )
    {
        handshake->isInRenderMode = true;
        handshake->onTextureRequested();
    }
}

void D3DManager::renderOverlay( void* d3dDev, const unsigned short resX, const unsigned short resY, const unsigned short* viewport, const unsigned char colorDepth, OverlayTextureManager* textureManager )
{
    if ( ( handshake != NULL ) && ( handshake->state == HANDSHAKE_STATE_COMPLETE ) && window_activated && handshake->isSessionRunning )
    {
        //logg( "renderOverlay()" );
        
        handshake->viewportX = viewport[0];
        handshake->viewportY = viewport[1];
        handshake->viewportWidth = viewport[2];
        handshake->viewportHeight = viewport[3];
        char result = handshake->jvmConn.telemFuncs.call_beforeRender( handshake->viewportX, handshake->viewportY, handshake->viewportWidth, handshake->viewportHeight );
        /*handshake->*/_checkRenderModeResult( "beforeRender()", result );
        
        if ( handshake->isInRenderMode/* && handshake->isInRealtime*/ )
        {
            result = handshake->jvmConn.inputFuncs.updateInput( &handshake->isPluginEnabled );
            /*handshake->*/_checkRenderModeResult( "updateInput()", result );
            
            if ( handshake->isInRenderMode && handshake->isPluginEnabled )
            {
                result = handshake->jvmConn.d3dFuncs.call_update();
                /*handshake->*/_checkRenderModeResult( "update()", result );
                
                if ( handshake->isInRenderMode )
                {
                    const float postScaleX = (float)resX / (float)viewport[2];
                    const float postScaleY = (float)resY / (float)viewport[3];
                    
                    textureManager->render( postScaleX, postScaleY, handshake->jvmConn.d3dFuncs.getNumTextures(), handshake->jvmConn.d3dFuncs.dirtyRectsBuffers, pixBuffCallback, handshake->jvmConn.d3dFuncs.textureVisibleFlags, handshake->jvmConn.d3dFuncs.rectangleVisibleFlags, handshake->jvmConn.d3dFuncs.textureIsTransformedFlags, handshake->jvmConn.d3dFuncs.textureTranslations, handshake->jvmConn.d3dFuncs.textureRotationCenters, handshake->jvmConn.d3dFuncs.textureRotations, handshake->jvmConn.d3dFuncs.textureScales, handshake->jvmConn.d3dFuncs.textureClipRects );
                }
            }
        }
    }
}

bool _initializeD3D()
{
    logg( "Starting up rfDynHUD Plugin (D3D part)..." );
    
    if ( handshake == NULL )
    {
        logg( "Handshake wasn't successful, even if it stated, is was." );
        return ( false );
    }
    
    if ( !handshake->doSanityCheck( RFACTOR_PATH, PLUGIN_PATH, fileBuffer ) )
    {
        logg( "Sanity check failed." );
        handshake->state = HANDSHAKE_STATE_ERROR;
        
        return ( false );
    }
    
    HWND hWnd = getWindowHandle();
    if ( hWnd == 0 )
    {
        logg( "ERROR: Unable to get the WindowHandle" );
        handshake->state = HANDSHAKE_STATE_ERROR;
        
        return ( false );
    }
    
    logg( "Initializing DirectInput..." );
    if ( initDirectInput( hWnd ) )
    {
        logg( "Successfully initialized DirectInput." );
    }
    else
    {
        logg( "DirectInput not initialized. Plugin won't work." );
        handshake->state = HANDSHAKE_STATE_ERROR;
        
        return ( false );
    }
    
    if ( !handshake->jvmConn.init( PLUGIN_PATH, resX, resY ) )
    {
        handshake->state = HANDSHAKE_STATE_ERROR;
        
        return ( false );
    }
    
    return ( true );
}

void D3DManager::initialize( void* d3dDev, const unsigned short _resX, const unsigned short _resY, const unsigned char colorDepth, const bool windowed, const unsigned short fullscreenRefreshHz, const HWND deviceWindowHandle, OverlayTextureManager* _textureManager )
{
    m_windowHandle = deviceWindowHandle;
    
    if ( ( handshake != NULL ) && ( handshake->state == HANDSHAKE_STATE_ERROR ) )
        return;
    
    fileBuffer = (char*)malloc( MAX_PATH );
    initPluginIniFilename( RFACTOR_PATH, PLUGIN_PATH );
    initLogFilename( RFACTOR_PATH, PLUGIN_PATH );
    
    resX = _resX;
    resY = _resY;
    
    loggResolution( resX, resY );
    
    textureManager = _textureManager;
    
    switch ( doHandshake( true, &handshake ) )
    {
        case 0: // Incomplete. (step 1 completed)
            handshake->initializeD3D = &_initializeD3D;
            break;
        case 1: // Complete. (step 2 completed)
            if ( _initializeD3D() )
            {
                if ( handshake->initializePlugin != NULL )
                    handshake->initializePlugin();
                else
                    handshake->state = HANDSHAKE_STATE_ERROR;
            }
            break;
    }
    
    if ( handshake != NULL )
    {
        handshake->onTextureRequested = &_onTextureRequested;
        handshake->onRealtimeEntered = &_onRealtimeEntered;
        handshake->onRealtimeExited = &_onRealtimeExited;
        handshake->checkRenderModeResult = &_checkRenderModeResult;
    }
}

void notifyOnWindowActivation( bool activated )
{
    //loggb( "activated: ", activated );
    window_activated = activated;
}

void D3DManager::preReset( void* d3dDev )
{
    //logg( "preReset()" );
}

void D3DManager::postReset( void* d3dDev, const unsigned short resX, const unsigned short resY, const unsigned char colorDepth, const bool windowed, const unsigned short fullscreenRefreshHz, const HWND deviceWindowHandle )
{
    //logg( "postReset()" );
    if ( ( handshake != NULL ) && ( handshake->state == HANDSHAKE_STATE_COMPLETE ) && handshake->isInRenderMode/* && handshake->isInRealtime*/ )
    {
        logg( "postReset(). Updating Textures..." );
        
        handshake->jvmConn.d3dFuncs.updateAllTextureInfos();
        if ( textureManager->setupTextures( handshake->jvmConn.d3dFuncs.getNumTextures(), handshake->jvmConn.d3dFuncs.textureSizes, handshake->jvmConn.d3dFuncs.numUsedRectangles, handshake->jvmConn.d3dFuncs.usedRectangles ) )
            logg( "Textures successfully updated." );
        else
            logg( "Textures update failed." );
    }
}

void D3DManager::release( void* d3dDev )
{
    //logg( "Release()" );
    if ( handshake != NULL )
    {
        if ( ( handshake->state == HANDSHAKE_STATE_COMPLETE ) || ( handshake->state == HANDSHAKE_STATE_PLUGIN_DISPOSED ) )
        {
            logg( "Disposing DirectInput..." );
            
            disposeDirectInput();
            
            logg( "Successfully disposed DirectInput." );
            
            if ( handshake->state == HANDSHAKE_STATE_PLUGIN_DISPOSED )
            {
                handshake->jvmConn.destroy();
                handshake->state = HANDSHAKE_STATE_DISPOSED;
            }
            else
            {
                handshake->state = HANDSHAKE_STATE_D3D_DISPOSED;
            }
        }
        
        handshake = NULL;
    }
}

bool InputCallback::resolveMappingConflict( const unsigned char action1, const unsigned char action2 ) { return ( true ); }
void InputCallback::onInputActionStateChanged( const unsigned char actionIndex, const bool state ) {}
