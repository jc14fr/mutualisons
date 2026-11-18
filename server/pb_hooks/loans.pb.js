onRecordCreateRequest((e) => {
    const STATUS_AVAILABLE = "available";
    const STATUS_LENT = "lent";

    const itemId = e.record.get("item");
    if (!itemId) {
        throw new BadRequestError("Veuillez choisir un objet.");
    }

    let item;
    try {
        item = $app.findRecordById("items", itemId);
    } catch (err) {
        throw new BadRequestError("Objet introuvable.");
    }

    const isSuper = e.hasSuperuserAuth();
    const userId = e.auth ? e.auth.id : "";

    if (!isSuper && (userId === "" || item.get("owner") !== userId)) {
        throw new BadRequestError("Vous ne pouvez prêter que vos objets.");
    }
    if (item.get("status") !== STATUS_AVAILABLE) {
        throw new BadRequestError("Cet objet n'est pas disponible.");
    }

    e.next();

    item.set("status", STATUS_LENT);
    $app.save(item);
}, "loans");

onRecordUpdateRequest((e) => {
    const STATUS_AVAILABLE = "available";
    const STATUS_LENT = "lent";

    e.next();

    if (e.record.get("returned_at")) {
        try {
            const item = $app.findRecordById("items", e.record.get("item"));
            if (item.get("status") === STATUS_LENT) {
                item.set("status", STATUS_AVAILABLE);
                $app.save(item);
            }
        } catch (err) {
            // objet absent : rien à remettre à jour
        }
    }
}, "loans");

onRecordDeleteRequest((e) => {
    const STATUS_AVAILABLE = "available";
    const STATUS_LENT = "lent";

    let item = null;
    try {
        item = $app.findRecordById("items", e.record.get("item"));
    } catch (err) {
        item = null;
    }

    e.next();

    if (item && item.get("status") === STATUS_LENT) {
        item.set("status", STATUS_AVAILABLE);
        $app.save(item);
    }
}, "loans");
