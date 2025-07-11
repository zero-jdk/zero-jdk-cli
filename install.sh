#!/usr/bin/env bash

set -e

PROJECT="julien-may/zero-jdk"
BINARY="zjdk"
LATEST_RELEASE_API="https://api.github.com/repos/$PROJECT/releases/latest"
RELEASES_URL="https://github.com/$PROJECT/releases"
INSTALL_DIR="/usr/local/bin"
STEP_WIDTH=70

error_exit() { 
    echo ""; echo "Error: $*" 1>&2; exit 1; 
}

verify_needed_commands() { 
    command -v "$1" >/dev/null 2>&1 || error_exit "Required: $1 not found."; 
}

pad_status() {
  local status="$1"
  local detail="$2"
  local padlen=$((STEP_WIDTH - $(awk '{print length}' <<<"$CUR_STEP_MSG")))
  if [ $padlen -lt 1 ]; then padlen=1; fi
  printf "%*s%s" "$padlen" "" "$status"
  if [ -n "$detail" ]; then
    printf " - %s" "$detail"
  fi
  printf "\n"
}

detect_os() {
  case "$(uname -s)" in
    Linux*)   echo "linux" ;;
    Darwin*)  echo "macos" ;;
    *) error_exit "Unsupported OS: $(uname -s)" ;;
  esac
}

detect_arch() {
  case "$(uname -m)" in
    x86_64|amd64)   echo "x64" ;;
    arm64|aarch64)  echo "arm64" ;;
    *) error_exit "Unsupported architecture: $(uname -m)" ;;
  esac
}

get_latest_version() {
  if command -v curl >/dev/null 2>&1; then
    curl --connect-timeout 30 --max-time 60 -s "$LATEST_RELEASE_API" 2>/dev/null | \
      awk -F'"' '/"tag_name":/ {print $4}'
  elif command -v wget >/dev/null 2>&1; then
    wget --timeout=30 --read-timeout=60 -qO- "$LATEST_RELEASE_API" 2>/dev/null | \
      awk -F'"' '/"tag_name":/ {print $4}'
  else
    error_exit "Either curl or wget is required to fetch the latest version."
  fi
}

download() {
  local url="$1"
  local out="$2"

  if command -v curl >/dev/null 2>&1; then
    curl --connect-timeout 30 --max-time 600 -fsSL "$url" -o "$out" 2>/dev/null
  elif command -v wget >/dev/null 2>&1; then
    wget --timeout=30 --read-timeout=600 -q "$url" -O "$out" 2>/dev/null
  else
    error_exit "Neither curl nor wget is available for downloads."
  fi
}

is_platform_supported() {
  local supported="macos-x64\macos-arm64\nlinux-arm64\nlinux-x64"

  local os="$1"
  local arch="$2"

  if echo "${supported}" | grep -q "${os}-${arch}"; then
    echo "true"
  fi
}


# Pre-Step: Verify needed commands
verify_needed_commands tar

echo "zero-jdk installer"
echo ""

# Step 1: Verify platform support
CUR_STEP_MSG="[1/6] Verify platform support..."
printf "%s" "$CUR_STEP_MSG"

OS=$(detect_os)
ARCH=$(detect_arch)

PLATFORM_SUPPORTED="${PLATFORM_SUPPORTED:-$(is_platform_supported "$OS" "$ARCH")}"
if [ -z "$PLATFORM_SUPPORTED" ]; then
  pad_status "failed"
  error_exit "Unsupported platform ${OS}-${ARCH}. You may build it from source, go to https://github.com/zero-jdk/cli"
else
  pad_status "done"
fi


# Step 2: Resolve latest version
CUR_STEP_MSG="[2/6] Resolving latest version..."
printf "%s" "$CUR_STEP_MSG"

VERSION="${VERSION:-$(get_latest_version)}"
if [ -z "$VERSION" ]; then
  pad_status "failed"
  error_exit "Failed to detect latest version."
else
  pad_status "done" "$VERSION"
fi

TARBALL="zjdk-${VERSION#v}-${OS}-${ARCH}.tar.gz"
CHECKSUMS="checksums_sha256.txt"
URL_TARBALL="$RELEASES_URL/download/$VERSION/$TARBALL"
URL_CHECKSUMS="$RELEASES_URL/download/$VERSION/$CHECKSUMS"


# Step 3: Download
CUR_STEP_MSG="[3/6] Downloading $TARBALL..."
printf "%s" "$CUR_STEP_MSG"

TMPDIR="$(mktemp -d)"
cleanup() { rm -rf "$TMPDIR"; }
trap cleanup EXIT

cd "$TMPDIR"

if download "$URL_TARBALL" "$TARBALL" && download "$URL_CHECKSUMS" "$CHECKSUMS"; then
  pad_status "done"
else
  pad_status "failed"
  echo ""
  echo "Could not download the binary from:"
  echo "  $URL_TARBALL"
  echo "Please check your internet connection or try again later."
  exit 1
fi


# Step 4: Verify checksum
CUR_STEP_MSG="[4/6] Verifying checksum..."
printf "%s" "$CUR_STEP_MSG"
if command -v openssl >/dev/null 2>&1; then
  SUM_EXPECTED=$(awk "/$TARBALL/ {print \$1}" "$CHECKSUMS")
  SUM_ACTUAL=$(openssl dgst -sha256 "$TARBALL" | awk '{print $2}')
  if [ "$SUM_EXPECTED" != "$SUM_ACTUAL" ]; then
    pad_status "failed"
    echo ""
    echo "Checksum verification FAILED!"
    echo "Expected: $SUM_EXPECTED"
    echo "Actual:   $SUM_ACTUAL"
    echo ""
    echo "Aborting installation."
    exit 1
  else
    pad_status "done"
  fi
else
  pad_status "skipped"
  echo ""
  echo "Warning: openssl is not installed. Checksum verification was skipped."
  echo "You should only continue if you trust the downloaded file."
  echo ""
  read -rp "Continue installation without verifying the checksum? [y/N]: " yn
  case $yn in
    [Yy]*) ;;
    *) echo "Installation aborted as requested by user."
       exit 1 ;;
  esac
fi


# Step 5: Extract
CUR_STEP_MSG="[5/6] Extracting..."
printf "%s" "$CUR_STEP_MSG"
if tar -xzf "$TARBALL"; then
  pad_status "done"
else
  pad_status "failed"
  echo ""
  echo "Could not extract $TARBALL."
  exit 1
fi


# Step 6: Install
CUR_STEP_MSG="[6/6] Installing to $INSTALL_DIR..."
FOUND_BINARY=$(find . -type f -name "$BINARY" | head -n1)
printf "%s" "$CUR_STEP_MSG"
if [ -z "$FOUND_BINARY" ]; then
  pad_status "failed"
  echo ""
  echo "Could not find the $BINARY binary after extracting the archive."
  echo "Aborting installation."
  exit 1
fi

if [ ! -w "$INSTALL_DIR" ]; then
  if sudo -n true 2>/dev/null; then
    # Sudo session is active, so no prompt
    if sudo cp "$FOUND_BINARY" "$INSTALL_DIR/$BINARY" && sudo chmod +x "$INSTALL_DIR/$BINARY"; then
      pad_status "done"
    else
      pad_status "failed"
      echo ""
      echo "Could not copy $BINARY to $INSTALL_DIR. You may lack the necessary permissions."
      echo "Try running the script again and ensure you have sudo/root access."
      exit 1
    fi
  else
    # Sudo will prompt
    printf "\n\n"
    if sudo cp "$FOUND_BINARY" "$INSTALL_DIR/$BINARY" && sudo chmod +x "$INSTALL_DIR/$BINARY"; then
      printf "\n%s" "$CUR_STEP_MSG"
      pad_status "done"
    else
      printf "\n%s" "$CUR_STEP_MSG"
      pad_status "failed"
      echo ""
      echo "Could not copy $BINARY to $INSTALL_DIR. You may have entered an incorrect password or lack the necessary permissions."
      echo "Try running the script again and ensure you have sudo/root access."
      exit 1
    fi
  fi
else
  if cp "$FOUND_BINARY" "$INSTALL_DIR/$BINARY" && chmod +x "$INSTALL_DIR/$BINARY"; then
    pad_status "done"
  else
    pad_status "failed"
    echo ""
    echo "Could not copy $BINARY to $INSTALL_DIR."
    exit 1
  fi
fi


# Done
echo ""
echo "Installation successful!"
echo ""
if ! command -v "$BINARY" >/dev/null 2>&1; then
  echo "Note: $INSTALL_DIR is not in your PATH."
  echo "You may want to add it before running '$BINARY'."
  echo ""
fi
echo "Run 'zjdk --help' to get started."
