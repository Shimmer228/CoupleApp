import { Response } from "express";
import { Prisma } from "@prisma/client";
import { prisma } from "../config/prisma";
import { AuthenticatedRequest } from "../types/auth-request";
import { getDayRange, parseRequiredDate } from "../utils/date";

export const eventInclude = {
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

export const getCurrentUserContext = async (userId: string) => {
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

type EventCreationInput = {
  userId: string;
  title: string;
  description?: string | null;
  date: Date;
};

export const createEventForUser = async (tx: Prisma.TransactionClient, input: EventCreationInput) => {
  const user = await tx.user.findUnique({
    where: { id: input.userId },
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

  const event = await tx.event.create({
    data: {
      title: input.title,
      description: input.description ?? null,
      date: input.date,
      pairId: user.pairId,
      createdById: input.userId,
    },
    include: eventInclude,
  });

  return { event };
};

const getEventForPair = async (eventId: string, pairId: string) => {
  const event = await prisma.event.findUnique({
    where: { id: eventId },
  });

  if (!event || event.pairId !== pairId) {
    throw new Error("EVENT_NOT_FOUND");
  }

  return event;
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

    if (error.message === "EVENT_NOT_FOUND") {
      return res.status(404).json({ message: "Event not found" });
    }

    if (error.message === "FORBIDDEN_EVENT_ACTION") {
      return res.status(403).json({ message: "Only the event creator can edit or delete this event" });
    }

    if (error.message === "EVENT_UPDATE_REQUIRED") {
      return res.status(400).json({ message: "Provide a title, description, or date to update" });
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

    const result = await prisma.$transaction((tx) =>
      createEventForUser(tx, {
        userId,
        title,
        description,
        date,
      })
    );

    return res.status(201).json(result);
  } catch (error) {
    return sendEventError(res, error);
  }
};

export const updateEvent = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const eventId = String(req.params.id ?? "");
    const hasTitle = Object.prototype.hasOwnProperty.call(req.body, "title");
    const hasDescription = Object.prototype.hasOwnProperty.call(req.body, "description");
    const hasDate = Object.prototype.hasOwnProperty.call(req.body, "date");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!hasTitle && !hasDescription && !hasDate) {
      throw new Error("EVENT_UPDATE_REQUIRED");
    }

    const user = await getCurrentUserContext(userId);
    const event = await getEventForPair(eventId, user.pairId!);

    if (event.createdById !== userId) {
      throw new Error("FORBIDDEN_EVENT_ACTION");
    }

    const title = hasTitle ? normalizeTitle(String(req.body.title ?? "")) : undefined;
    const description = hasDescription ? normalizeDescription(req.body.description) : undefined;
    const date = hasDate ? parseRequiredDate(req.body.date) : undefined;

    if (hasTitle && !title) {
      return res.status(400).json({ message: "Event title is required" });
    }

    const updatedEvent = await prisma.event.update({
      where: { id: event.id },
      data: {
        ...(hasTitle ? { title } : {}),
        ...(hasDescription ? { description } : {}),
        ...(hasDate ? { date } : {}),
      },
      include: eventInclude,
    });

    return res.json({ event: updatedEvent });
  } catch (error) {
    return sendEventError(res, error);
  }
};

export const deleteEvent = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const eventId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const user = await getCurrentUserContext(userId);
    const event = await getEventForPair(eventId, user.pairId!);

    if (event.createdById !== userId) {
      throw new Error("FORBIDDEN_EVENT_ACTION");
    }

    await prisma.event.delete({
      where: { id: event.id },
    });

    return res.json({ deletedId: event.id });
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
