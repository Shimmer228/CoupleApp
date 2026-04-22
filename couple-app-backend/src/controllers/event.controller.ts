import { Response } from "express";
import { Prisma } from "@prisma/client";
import { prisma } from "../config/prisma";
import { AuthenticatedRequest } from "../types/auth-request";
import { getDayRange, parseRequiredDate } from "../utils/date";

const eventInclude = {
  createdBy: {
    select: {
      id: true,
      email: true,
    },
  },
} satisfies Prisma.EventInclude;

const eventOrderBy = {
  date: "asc",
} satisfies Prisma.EventOrderByWithRelationInput;

const normalizeTitle = (value: string) => value.trim();
const normalizeDescription = (value: unknown) => {
  const normalized = String(value ?? "").trim();
  return normalized || null;
};

const getCurrentUserContext = async (userId: string) => {
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

  return user;
};

const sendEventError = (res: Response, error: unknown) => {
  if (error instanceof Error) {
    if (error.message === "USER_NOT_FOUND") {
      return res.status(404).json({ message: "User not found" });
    }

    if (error.message === "PAIR_REQUIRED") {
      return res.status(400).json({ message: "You need to be connected to a pair first" });
    }

    if (error.message === "DATE_REQUIRED") {
      return res.status(400).json({ message: "Event date is required" });
    }

    if (error.message === "INVALID_DATE" || error.message === "INVALID_DATE_QUERY") {
      return res.status(400).json({ message: "Invalid date format" });
    }
  }

  console.error("Event controller error:", error);
  return res.status(500).json({ message: "Internal server error" });
};

export const createEvent = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const title = normalizeTitle(String(req.body.title ?? ""));
    const description = normalizeDescription(req.body.description);

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!title) {
      return res.status(400).json({ message: "Event title is required" });
    }

    const date = parseRequiredDate(req.body.date);
    const user = await getCurrentUserContext(userId);

    const event = await prisma.event.create({
      data: {
        title,
        description,
        date,
        pairId: user.pairId!,
        createdById: userId,
      },
      include: eventInclude,
    });

    return res.status(201).json({ event });
  } catch (error) {
    return sendEventError(res, error);
  }
};

export const getEventsForDate = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const date = String(req.query.date ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const user = await getCurrentUserContext(userId);
    const { start, end } = getDayRange(date);

    const events = await prisma.event.findMany({
      where: {
        pairId: user.pairId!,
        date: {
          gte: start,
          lte: end,
        },
      },
      include: eventInclude,
      orderBy: eventOrderBy,
    });

    return res.json({ events });
  } catch (error) {
    return sendEventError(res, error);
  }
};

export const getAllEvents = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const user = await getCurrentUserContext(userId);

    const events = await prisma.event.findMany({
      where: { pairId: user.pairId! },
      include: eventInclude,
      orderBy: eventOrderBy,
    });

    return res.json({ events });
  } catch (error) {
    return sendEventError(res, error);
  }
};
