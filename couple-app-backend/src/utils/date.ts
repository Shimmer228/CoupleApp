export const parseOptionalDate = (value: unknown) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const parsedDate = new Date(String(value));

  if (Number.isNaN(parsedDate.getTime())) {
    throw new Error("INVALID_DATE");
  }

  return parsedDate;
};

export const parseRequiredDate = (value: unknown) => {
  const parsedDate = parseOptionalDate(value);

  if (!parsedDate) {
    throw new Error("DATE_REQUIRED");
  }

  return parsedDate;
};

export const getDayRange = (dateInput: string) => {
  const normalized = dateInput.trim();

  if (!/^\d{4}-\d{2}-\d{2}$/.test(normalized)) {
    throw new Error("INVALID_DATE_QUERY");
  }

  const start = new Date(`${normalized}T00:00:00.000Z`);
  const end = new Date(`${normalized}T23:59:59.999Z`);

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    throw new Error("INVALID_DATE_QUERY");
  }

  return { start, end };
};
