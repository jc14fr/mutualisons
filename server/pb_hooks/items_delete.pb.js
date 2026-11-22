onRecordDeleteRequest((e) => {
    const maintenant = new Date();
    const prets = $app.findRecordsByFilter(
        "loans", "item = {:i}", "", 200, 0, { i: e.record.id }
    ).filter((p) => p.getString("returned_at") === "");

    const enCours = prets.some((p) => {
        const debut = p.getString("start_date");
        return !debut || new Date(debut.replace(" ", "T")) <= maintenant;
    });
    if (enCours) {
        throw new BadRequestError("Cet objet est prêté. Il faut d'abord le récupérer.");
    }
    e.next();
}, "items");