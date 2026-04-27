import fs from "fs";
import path from "path";

const uploadsDirectory = path.join(process.cwd(), "uploads");
const avatarsDirectory = path.join(uploadsDirectory, "avatars");

export const ensureAvatarUploadDir = () => {
  fs.mkdirSync(avatarsDirectory, { recursive: true });
};

export const buildAvatarUrl = (filename: string) => `/uploads/avatars/${filename}`;

export const deleteLocalAvatarFile = (avatarUrl: string | null | undefined) => {
  if (!avatarUrl || !avatarUrl.startsWith("/uploads/avatars/")) {
    return;
  }

  const relativePath = avatarUrl.replace(/^\/+/, "");
  const absolutePath = path.join(process.cwd(), relativePath);

  if (fs.existsSync(absolutePath)) {
    fs.unlinkSync(absolutePath);
  }
};
