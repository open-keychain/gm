#!/bin/bash

APP_DIR=../app/src/main
DRAWABLE_DIR=$APP_DIR/res/mipmap
MDPI_DIR=$APP_DIR/res/mipmap-mdpi
HDPI_DIR=$APP_DIR/res/mipmap-hdpi
XDPI_DIR=$APP_DIR/res/mipmap-xhdpi
XXDPI_DIR=$APP_DIR/res/mipmap-xxhdpi
XXXDPI_DIR=$APP_DIR/res/mipmap-xxxhdpi
PLAY_DIR=./mipmaps/
SRC_DIR=./mipmaps/

NAME="ic_launcher"

inkscape -w 48 -h 48 -e "$MDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 72 -h 72 -e "$HDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 96 -h 96 -e "$XDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 144 -h 144 -e "$XXDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 192 -h 192 -e "$XXXDPI_DIR/$NAME.png" $NAME.svg
inkscape -w 512 -h 512 -e "$PLAY_DIR/$NAME.png" $NAME.svg
