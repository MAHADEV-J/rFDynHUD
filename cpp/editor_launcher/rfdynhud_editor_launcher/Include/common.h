#ifndef _COMMON_H
#define _COMMON_H

void initPluginIniFilename();
unsigned short getPluginIniSetting( const char* group, const char* setting, const char* defaultValue, char* result, const unsigned short bufferLength );
unsigned short getFolderFromPluginIni( const char* group, const char* setting, const char* defaultValue, char* result, const unsigned short bufferLength );

#endif // _COMMON_H
