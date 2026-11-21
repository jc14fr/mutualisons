onRecordCreateRequest((e) => {
    const pbDate = (d) => d.toISOString().replace("T", " ");
    const parseDate = (v) => new Date(String(v).replace(" ", "T"));
    const conflit = (itemId, debut, fin) => {
        const prets = $app.findRecordsByFilter(
            "loans",
            "item = {:i} && returned_at = '' && start_date < {:f} && due_date > {:d}",
            "", 1, 0,
            { i: itemId, f: pbDate(fin), d: pbDate(debut) }
        );
        return prets.length > 0;
    };

    let item;
    try {
        item = $app.findRecordById("items", e.record.get("item"));
    } catch (err) {
        throw new BadRequestError("Objet introuvable.");
    }

    if (item.get("owner") === e.auth.id) {
        throw new BadRequestError("Vous êtes le propriétaire de cet objet.");
    }
    if (item.get("status") === "hidden") {
        throw new BadRequestError("Cet objet n'est pas disponible au prêt.");
    }

    const mesCercles = $app.findRecordsByFilter(
        "circles", "members.id ?= {:u}", "", 500, 0, { u: e.auth.id }
    ).map((c) => c.id);
    const partages = item.getStringSlice("circles");
    if (!partages.some((c) => mesCercles.includes(c))) {
        throw new BadRequestError("Cet objet n'est pas partagé avec vos cercles.");
    }

    const debut = parseDate(e.record.get("start_date"));
    const fin = parseDate(e.record.get("due_date"));
    if (isNaN(debut.getTime()) || isNaN(fin.getTime())) {
        throw new BadRequestError("Dates invalides.");
    }
    if (!(debut < fin)) {
        throw new BadRequestError("La date de fin doit être après la date de début.");
    }
    const aujourdhui = new Date();
    aujourdhui.setUTCHours(0, 0, 0, 0);
    if (debut < aujourdhui) {
        throw new BadRequestError("La date de début ne peut pas être dans le passé.");
    }
    if (conflit(item.id, debut, fin)) {
        throw new BadRequestError("Cet objet est déjà réservé sur cette période.");
    }

    const doublon = $app.findRecordsByFilter(
        "loan_requests",
        "item = {:i} && requester = {:u} && status = 'pending'",
        "", 1, 0, { i: item.id, u: e.auth.id }
    );
    if (doublon.length > 0) {
        throw new BadRequestError("Vous avez déjà une demande en attente pour cet objet.");
    }

    e.record.set("requester", e.auth.id);
    e.record.set("owner", item.get("owner"));
    e.record.set("status", "pending");
    e.next();
}, "loan_requests");

onRecordUpdateRequest((e) => {
    const pbDate = (d) => d.toISOString().replace("T", " ");
    const parseDate = (v) => new Date(String(v).replace(" ", "T"));
    const conflit = (itemId, debut, fin) => {
        const prets = $app.findRecordsByFilter(
            "loans",
            "item = {:i} && returned_at = '' && start_date < {:f} && due_date > {:d}",
            "", 1, 0,
            { i: itemId, f: pbDate(fin), d: pbDate(debut) }
        );
        return prets.length > 0;
    };

    const avant = e.record.original().get("status");
    const apres = e.record.get("status");

    if (avant === apres || avant !== "pending") {
        e.next();
        return;
    }

    if (!e.hasSuperuserAuth() && e.auth.id !== e.record.get("owner")) {
        throw new BadRequestError("Seul le propriétaire peut répondre à cette demande.");
    }

    if (apres === "accepted") {
        const item = $app.findRecordById("items", e.record.get("item"));
        const debut = parseDate(e.record.get("start_date"));
        const fin = parseDate(e.record.get("due_date"));

        if (conflit(item.id, debut, fin)) {
            throw new BadRequestError("Ces dates chevauchent un prêt déjà accepté.");
        }

        e.next();

        const loan = new Record($app.findCollectionByNameOrId("loans"));
        loan.set("item", item.id);
        loan.set("lender", e.record.get("owner"));
        loan.set("borrower", e.record.get("requester"));
        loan.set("start_date", pbDate(debut));
        loan.set("due_date", pbDate(fin));
        $app.save(loan);

        // Le statut "prêté" ne s'applique qu'à partir du début du prêt
        if (debut <= new Date()) {
            item.set("status", "lent");
            $app.save(item);
        }
        return;
    }

    e.next();
}, "loan_requests");