onRecordDeleteRequest((e) => {
    if (e.record.get("status") === "lent") {
        throw new BadRequestError("Cet objet est prêté. Il faut d'abord le récupérer.");
    }
    e.next();
}, "items");