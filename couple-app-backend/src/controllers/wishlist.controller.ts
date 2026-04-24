import { Prisma, TransactionCategory, TransactionType, WishlistPriority } from "@prisma/client";
import { Response } from "express";
import { prisma } from "../config/prisma";
import { AuthenticatedRequest } from "../types/auth-request";

const wishlistInclude = {
  createdBy: {
    select: {
      id: true,
      email: true,
    },
  },
} satisfies Prisma.WishlistItemInclude;

const wishlistOrderBy = {
  createdAt: "desc",
} satisfies Prisma.WishlistItemOrderByWithRelationInput;

const normalizeTitle = (value: string) => value.trim();
const normalizeUrl = (value: string) => value.trim();

const parseCategory = (value: unknown) => {
  const normalized = String(value ?? "").trim().toUpperCase();

  if (
    normalized === TransactionCategory.SELF ||
    normalized === TransactionCategory.PARTNER ||
    normalized === TransactionCategory.SHARED
  ) {
    return normalized;
  }

  throw new Error("INVALID_WISHLIST_CATEGORY");
};

const parsePriority = (value: unknown) => {
  const normalized = String(value ?? "").trim().toUpperCase();

  if (
    normalized === WishlistPriority.LOW ||
    normalized === WishlistPriority.MEDIUM ||
    normalized === WishlistPriority.HIGH
  ) {
    return normalized;
  }

  throw new Error("INVALID_WISHLIST_PRIORITY");
};

const getPairContext = async (userId: string) => {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: {
      id: true,
      pairId: true,
    },
  });

  if (!user) {
    throw new Error("USER_NOT_FOUND");
  }

  if (!user.pairId) {
    throw new Error("PAIR_REQUIRED");
  }

  return {
    userId: user.id,
    pairId: user.pairId,
  };
};

const sendWishlistError = (res: Response, error: unknown) => {
  if (error instanceof Error) {
    if (error.message === "USER_NOT_FOUND") {
      return res.status(404).json({ message: "User not found" });
    }

    if (error.message === "PAIR_REQUIRED") {
      return res.status(400).json({ message: "You need to be connected to a pair first" });
    }

    if (error.message === "INVALID_WISHLIST_PRIORITY") {
      return res.status(400).json({ message: "Invalid wishlist priority" });
    }

    if (error.message === "INVALID_WISHLIST_CATEGORY") {
      return res.status(400).json({ message: "Invalid wishlist category" });
    }

    if (error.message === "WISHLIST_ITEM_NOT_FOUND") {
      return res.status(404).json({ message: "Wishlist item not found" });
    }

    if (error.message === "ALREADY_PURCHASED") {
      return res.status(400).json({ message: "Wishlist item has already been purchased" });
    }
  }

  console.error("Wishlist controller error:", error);
  return res.status(500).json({ message: "Internal server error" });
};

export const createWishlistItem = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const title = normalizeTitle(String(req.body.title ?? ""));
    const rawPrice = req.body.price;
    const price = rawPrice === undefined || rawPrice === null || rawPrice === "" ? null : Number(rawPrice);
    const url = normalizeUrl(String(req.body.url ?? ""));

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!title) {
      return res.status(400).json({ message: "Wishlist title is required" });
    }

    if (price !== null && (!Number.isFinite(price) || price < 0)) {
      return res.status(400).json({ message: "Price must be 0 or greater" });
    }

    const priority = parsePriority(req.body.priority);
    const category = parseCategory(req.body.category);
    const pairContext = await getPairContext(userId);

    const item = await prisma.wishlistItem.create({
      data: {
        title,
        url: url || null,
        price,
        priority,
        category,
        createdById: userId,
        pairId: pairContext.pairId,
      },
      include: wishlistInclude,
    });

    return res.status(201).json({ item });
  } catch (error) {
    return sendWishlistError(res, error);
  }
};

export const getWishlistItems = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const pairContext = await getPairContext(userId);

    const items = await prisma.wishlistItem.findMany({
      where: { pairId: pairContext.pairId },
      include: wishlistInclude,
      orderBy: wishlistOrderBy,
    });

    return res.json({
      currentUserId: userId,
      items,
    });
  } catch (error) {
    return sendWishlistError(res, error);
  }
};

export const purchaseWishlistItem = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const itemId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const pairContext = await getPairContext(userId);
    const existingItem = await prisma.wishlistItem.findUnique({
      where: { id: itemId },
    });

    if (!existingItem || existingItem.pairId !== pairContext.pairId) {
      throw new Error("WISHLIST_ITEM_NOT_FOUND");
    }

    if (existingItem.isPurchased) {
      throw new Error("ALREADY_PURCHASED");
    }

    const createTransaction = Boolean(req.body.createTransaction);

    const result = await prisma.$transaction(async (tx) => {
      const purchasedItem = await tx.wishlistItem.update({
        where: { id: existingItem.id },
        data: {
          isPurchased: true,
        },
        include: wishlistInclude,
      });

      const transaction =
        createTransaction && existingItem.price !== null
          ? await tx.transaction.create({
              data: {
                title: existingItem.title,
                amount: existingItem.price,
                type: TransactionType.EXPENSE,
                category: existingItem.category,
                createdById: userId,
                pairId: existingItem.pairId,
              },
            })
          : null;

      return {
        item: purchasedItem,
        transaction,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendWishlistError(res, error);
  }
};

export const deleteWishlistItem = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const itemId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const pairContext = await getPairContext(userId);
    const existingItem = await prisma.wishlistItem.findUnique({
      where: { id: itemId },
      select: {
        id: true,
        pairId: true,
      },
    });

    if (!existingItem || existingItem.pairId !== pairContext.pairId) {
      throw new Error("WISHLIST_ITEM_NOT_FOUND");
    }

    await prisma.wishlistItem.delete({
      where: { id: existingItem.id },
    });

    return res.json({ deletedId: existingItem.id });
  } catch (error) {
    return sendWishlistError(res, error);
  }
};
