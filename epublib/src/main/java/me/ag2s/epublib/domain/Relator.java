package me.ag2s.epublib.domain;


/**
 * A relator denotes which role a certain individual had in the creation/modification of the ebook.
 *
 * Examples are 'creator', 'blurb writer', etc.
 *
 * This is contains the complete Library of Concress relator list.
 *
 * @see <a href="http://www.loc.gov/marc/relators/relaterm.html">MARC Code List for Relators</a>
 *
 * @author paul
 */
public enum Relator {

  /**
   * Use for a person or organization who principally exhibits acting skills in a musical or dramatic presentation or entertainment.
   */
  ACTOR("act", "Actor"),

  /**
   * Use for a person or organization who 1) reworks a musical composition, usually for a different medium, or 2) rewrites novels or stories for motion pictures or other audiovisual medium.
   */
  ADAPTER("adp", "Adapter"),

  /**
   * Use for a person or organization that reviews, examines and interprets data or information in a specific area.
   */
  ANALYST("anl", "Analyst"),

  /**
   * Use for a person or organization who draws the two-dimensional figures, manipulates the three dimensional objects and/or also programs the computer to move objects and images for the purpose of animated film processing. Animation cameras, stands, celluloid screens, transparencies and inks are some of the tools of the animator.
   */
  ANIMATOR("anm", "Animator"),

  /**
   * Use for a person who writes manuscript annotations on a printed item.
   */
  ANNOTATOR("ann", "Annotator"),

  /**
   * Use for a person or organization responsible for the submission of an application or who is named as eligible for the results of the processing of the application (e.g., bestowing of rights, reward, title, position).
   */
  APPLICANT("app", "Applicant"),

  /**
   * Use for a person or organization who designs structures or oversees their construction.
   */
  ARCHITECT("arc", "Architect"),

  /**
   * Use for a person or organization who transcribes a musical composition, usually for a different medium from that of the original; in an arrangement the musical substance remains essentially unchanged.
   */
  ARRANGER("arr", "Arranger"),

  /**
   * Use for a person (e.g., a painter or sculptor) who makes copies of works of visual art.
   */
  ART_COPYIST("acp", "Art copyist"),

  /**
   * Use for a person (e.g., a painter) or organization who conceives, and perhaps also implements, an original graphic design or work of art, if specific codes (e.g., [egr], [etr]) are not desired. For book illustrators, prefer Illustrator [ill].
   */
  ARTIST("art", "Artist"),

  /**
   * Use for a person responsible for controlling the development of the artistic style of an entire production, including the choice of works to be presented and selection of senior production staff.
   */
  ARTISTIC_DIRECTOR("ard", "Artistic director"),

  /**
   * Use for a person or organization to whom a license for printing or publishing has been transferred.
   */
  ASSIGNEE("asg", "Assignee"),

  /**
   * Use for a person or organization associated with or found in an item or collection, which cannot be determined to be that of a Former owner [fmo] or other designated relator indicative of provenance.
   */
  ASSOCIATED_NAME("asn", "Associated name"),

  /**
   * Use for an author, artist, etc., relating him/her to a work for which there is or once was substantial authority for designating that person as author, creator, etc. of the work.
   */
  ATTRIBUTED_NAME("att", "Attributed name"),

  /**
   * Use for a person or organization in charge of the estimation and public auctioning of goods, particularly books, artistic works, etc.
   */
  AUCTIONEER("auc", "Auctioneer"),

  /**
   * Use for a person or organization chiefly responsible for the intellectual or artistic content of a work, usually printed text. This term may also be used when more than one person or body bears such responsibility.
   */
  AUTHOR("aut", "Author"),

  /**
   * Use for a person or organization whose work is largely quoted or extracted in works to which he or she did not contribute directly. Such quotations are found particularly in exhibition catalogs, collections of photographs, etc.
   */
  AUTHOR_IN_QUOTATIONS_OR_TEXT_EXTRACTS("aqt",
      "Author in quotations or text extracts"),

  /**
   * Use for a person or organization responsible for an afterword, postface, colophon, etc. but who is not the chief author of a work.
   */
  AUTHOR_OF_AFTERWORD_COLOPHON_ETC("aft",
      "Author of afterword, colophon, etc."),

  /**
   * Use for a person or organization responsible for the dialog or spoken commentary for a screenplay or sound recording.
   */
  AUTHOR_OF_DIALOG("aud", "Author of dialog"),

  /**
   * Use for a person or organization responsible for an introduction, preface, foreword, or other critical introductory matter, but who is not the chief author.
   */
  AUTHOR_OF_INTRODUCTION_ETC("aui", "Author of introduction, etc."),

  /**
   * Use for a person or organization responsible for a motion picture screenplay, dialog, spoken commentary, etc.
   */
  AUTHOR_OF_SCREENPLAY_ETC("aus", "Author of screenplay, etc."),

  /**
   * Use for a person or organization responsible for a work upon which the work represented by the catalog record is based. This may be appropriate for adaptations, sequels, continuations, indexes, etc.
   */
  BIBLIOGRAPHIC_ANTECEDENT("ant", "Bibliographic antecedent"),

  /**
   * Use for a person or organization responsible for the binding of printed or manuscript materials.
   */
  BINDER("bnd", "Binder"),

  /**
   * Use for a person or organization responsible for the binding design of a book, including the type of binding, the type of materials used, and any decorative aspects of the binding.
   */
  BINDING_DESIGNER("bdd", "Binding designer"),

  /**
   * Use for the named entity responsible for writing a commendation or testimonial for a work, which appears on or within the publication itself, frequently on the back or dust jacket of print publications or on advertising material for all media.
   */
  BLURB_WRITER("blw", "Blurb writer"),

  /**
   * Use for a person or organization responsible for the entire graphic design of a book, including arrangement of type and illustration, choice of materials, and process used.
   */
  BOOK_DESIGNER("bkd", "Book designer"),

  /**
   * Use for a person or organization responsible for the production of books and other print media, if specific codes (e.g., [bkd], [egr], [tyd], [prt]) are not desired.
   */
  BOOK_PRODUCER("bkp", "Book producer"),

  /**
   * Use for a person or organization responsible for the design of flexible covers designed for or published with a book, including the type of materials used, and any decorative aspects of the bookjacket.
   */
  BOOKJACKET_DESIGNER("bjd", "Bookjacket designer"),

  /**
   * Use for a person or organization responsible for the design of a book owner's identification label that is most commonly pasted to the inside front cover of a book.
   */
  BOOKPLATE_DESIGNER("bpd", "Bookplate designer"),

  /**
   * Use for a person or organization who makes books and other bibliographic materials available for purchase. Interest in the materials is primarily lucrative.
   */
  BOOKSELLER("bsl", "Bookseller"),

  /**
   * Use for a person or organization who writes in an artistic hand, usually as a copyist and or engrosser.
   */
  CALLIGRAPHER("cll", "Calligrapher"),

  /**
   * Use for a person or organization responsible for the creation of maps and other cartographic materials.
   */
  CARTOGRAPHER("ctg", "Cartographer"),

  /**
   * Use for a censor, bowdlerizer, expurgator, etc., official or private.
   */
  CENSOR("cns", "Censor"),

  /**
   * Use for a person or organization who composes or arranges dances or other movements (e.g., "master of swords") for a musical or dramatic presentation or entertainment.
   */
  CHOREOGRAPHER("chr", "Choreographer"),

  /**
   * Use for a person or organization who is in charge of the images captured for a motion picture film. The cinematographer works under the supervision of a director, and may also be referred to as director of photography. Do not confuse with videographer.
   */
  CINEMATOGRAPHER("cng", "Cinematographer"),

  /**
   * Use for a person or organization for whom another person or organization is acting.
   */
  CLIENT("cli", "Client"),

  /**
   * Use for a person or organization that takes a limited part in the elaboration of a work of another person or organization that brings complements (e.g., appendices, notes) to the work.
   */
  COLLABORATOR("clb", "Collaborator"),

  /**
   * Use for a person or organization who has brought together material from various sources that has been arranged, described, and cataloged as a collection. A collector is neither the creator of the material nor a person to whom manuscripts in the collection may have been addressed.
   */
  COLLECTOR("col", "Collector"),

  /**
   * Use for a person or organization responsible for the production of photographic prints from film or other colloid that has ink-receptive and ink-repellent surfaces.
   */
  COLLOTYPER("clt", "Collotyper"),

  /**
   * Use for the named entity responsible for applying color to drawings, prints, photographs, maps, moving images, etc.
   */
  COLORIST("clr", "Colorist"),

  /**
   * Use for a person or organization who provides interpretation, analysis, or a discussion of the subject matter on a recording, motion picture, or other audiovisual medium.
   */
  COMMENTATOR("cmm", "Commentator"),

  /**
   * Use for a person or organization responsible for the commentary or explanatory notes about a text. For the writer of manuscript annotations in a printed book, use Annotator [ann].
   */
  COMMENTATOR_FOR_WRITTEN_TEXT("cwt", "Commentator for written text"),

  /**
   * Use for a person or organization who produces a work or publication by selecting and putting together material from the works of various persons or bodies.
   */
  COMPILER("com", "Compiler"),

  /**
   * Use for the party who applies to the courts for redress, usually in an equity proceeding.
   */
  COMPLAINANT("cpl", "Complainant"),

  /**
   * Use for a complainant who takes an appeal from one court or jurisdiction to another to reverse the judgment, usually in an equity proceeding.
   */
  COMPLAINANT_APPELLANT("cpt", "Complainant-appellant"),

  /**
   * Use for a complainant against whom an appeal is taken from one court or jurisdiction to another to reverse the judgment, usually in an equity proceeding.
   */
  COMPLAINANT_APPELLEE("cpe", "Complainant-appellee"),

  /**
   * Use for a person or organization who creates a musical work, usually a piece of music in manuscript or printed form.
   */
  COMPOSER("cmp", "Composer"),

  /**
   * Use for a person or organization responsible for the creation of metal slug, or molds made of other materials, used to produce the text and images in printed matter.
   */
  COMPOSITOR("cmt", "Compositor"),

  /**
   * Use for a person or organization responsible for the original idea on which a work is based, this includes the scientific author of an audio-visual item and the conceptor of an advertisement.
   */
  CONCEPTOR("ccp", "Conceptor"),

  /**
   * Use for a person who directs a performing group (orchestra, chorus, opera, etc.) in a musical or dramatic presentation or entertainment.
   */
  CONDUCTOR("cnd", "Conductor"),

  /**
   * Use for the named entity responsible for documenting, preserving, or treating printed or manuscript material, works of art, artifacts, or other media.
   */
  CONSERVATOR("con", "Conservator"),

  /**
   * Use for a person or organization relevant to a resource, who is called upon for professional advice or services in a specialized field of knowledge or training.
   */
  CONSULTANT("csl", "Consultant"),

  /**
   * Use for a person or organization relevant to a resource, who is engaged specifically to provide an intellectual overview of a strategic or operational task and by analysis, specification, or instruction, to create or propose a cost-effective course of action or solution.
   */
  CONSULTANT_TO_A_PROJECT("csp", "Consultant to a project"),

  /**
   * Use for the party who opposes, resists, or disputes, in a court of law, a claim, decision, result, etc.
   */
  CONTESTANT("cos", "Contestant"),

  /**
   * Use for a contestant who takes an appeal from one court of law or jurisdiction to another to reverse the judgment.
   */
  CONTESTANT_APPELLANT("cot", "Contestant-appellant"),

  /**
   * Use for a contestant against whom an appeal is taken from one court of law or jurisdiction to another to reverse the judgment.
   */
  CONTESTANT_APPELLEE("coe", "Contestant-appellee"),

  /**
   * Use for the party defending a claim, decision, result, etc. being opposed, resisted, or disputed in a court of law.
   */
  CONTESTEE("cts", "Contestee"),

  /**
   * Use for a contestee who takes an appeal from one court or jurisdiction to another to reverse the judgment.
   */
  CONTESTEE_APPELLANT("ctt", "Contestee-appellant"),

  /**
   * Use for a contestee against whom an appeal is taken from one court or jurisdiction to another to reverse the judgment.
   */
  CONTESTEE_APPELLEE("cte", "Contestee-appellee"),

  /**
   * Use for a person or organization relevant to a resource, who enters into a contract with another person or organization to perform a specific task.
   */
  CONTRACTOR("ctr", "Contractor"),

  /**
   * Use for a person or organization one whose work has been contributed to a larger work, such as an anthology, serial publication, or other compilation of individual works. Do not use if the sole function in relation to a work is as author, editor, compiler or translator.
   */
  CONTRIBUTOR("ctb", "Contributor"),

  /**
   * Use for a person or organization listed as a copyright owner at the time of registration. Copyright can be granted or later transferred to another person or organization, at which time the claimant becomes the copyright holder.
   */
  COPYRIGHT_CLAIMANT("cpc", "Copyright claimant"),

  /**
   * Use for a person or organization to whom copy and legal rights have been granted or transferred for the intellectual content of a work. The copyright holder, although not necessarily the creator of the work, usually has the exclusive right to benefit financially from the sale and use of the work to which the associated copyright protection applies.
   */
  COPYRIGHT_HOLDER("cph", "Copyright holder"),

  /**
   * Use for a person or organization who is a corrector of manuscripts, such as the scriptorium official who corrected the work of a scribe. For printed matter, use Proofreader.
   */
  CORRECTOR("crr", "Corrector"),

  /**
   * Use for a person or organization who was either the writer or recipient of a letter or other communication.
   */
  CORRESPONDENT("crp", "Correspondent"),

  /**
   * Use for a person or organization who designs or makes costumes, fixes hair, etc., for a musical or dramatic presentation or entertainment.
   */
  COSTUME_DESIGNER("cst", "Costume designer"),

  /**
   * Use for a person or organization responsible for the graphic design of a book cover, album cover, slipcase, box, container, etc. For a person or organization responsible for the graphic design of an entire book, use Book designer; for book jackets, use Bookjacket designer.
   */
  COVER_DESIGNER("cov", "Cover designer"),

  /**
   * Use for a person or organization responsible for the intellectual or artistic content of a work.
   */
  CREATOR("cre", "Creator"),

  /**
   * Use for a person or organization responsible for conceiving and organizing an exhibition.
   */
  CURATOR_OF_AN_EXHIBITION("cur", "Curator of an exhibition"),

  /**
   * Use for a person or organization who principally exhibits dancing skills in a musical or dramatic presentation or entertainment.
   */
  DANCER("dnc", "Dancer"),

  /**
   * Use for a person or organization that submits data for inclusion in a database or other collection of data.
   */
  DATA_CONTRIBUTOR("dtc", "Data contributor"),

  /**
   * Use for a person or organization responsible for managing databases or other data sources.
   */
  DATA_MANAGER("dtm", "Data manager"),

  /**
   * Use for a person or organization to whom a book, manuscript, etc., is dedicated (not the recipient of a gift).
   */
  DEDICATEE("dte", "Dedicatee"),

  /**
   * Use for the author of a dedication, which may be a formal statement or in epistolary or verse form.
   */
  DEDICATOR("dto", "Dedicator"),

  /**
   * Use for the party defending or denying allegations made in a suit and against whom relief or recovery is sought in the courts, usually in a legal action.
   */
  DEFENDANT("dfd", "Defendant"),

  /**
   * Use for a defendant who takes an appeal from one court or jurisdiction to another to reverse the judgment, usually in a legal action.
   */
  DEFENDANT_APPELLANT("dft", "Defendant-appellant"),

  /**
   * Use for a defendant against whom an appeal is taken from one court or jurisdiction to another to reverse the judgment, usually in a legal action.
   */
  DEFENDANT_APPELLEE("dfe", "Defendant-appellee"),

  /**
   * Use for the organization granting a degree for which the thesis or dissertation described was presented.
   */
  DEGREE_GRANTOR("dgg", "Degree grantor"),

  /**
   * Use for a person or organization executing technical drawings from others' designs.
   */
  DELINEATOR("dln", "Delineator"),

  /**
   * Use for an entity depicted or portrayed in a work, particularly in a work of art.
   */
  DEPICTED("dpc", "Depicted"),

  /**
   * Use for a person or organization placing material in the physical custody of a library or repository without transferring the legal title.
   */
  DEPOSITOR("dpt", "Depositor"),

  /**
   * Use for a person or organization responsible for the design if more specific codes (e.g., [bkd], [tyd]) are not desired.
   */
  DESIGNER("dsr", "Designer"),

  /**
   * Use for a person or organization who is responsible for the general management of a work or who supervises the production of a performance for stage, screen, or sound recording.
   */
  DIRECTOR("drt", "Director"),

  /**
   * Use for a person who presents a thesis for a university or higher-level educational degree.
   */
  DISSERTANT("dis", "Dissertant"),

  /**
   * Use for the name of a place from which a resource, e.g., a serial, is distributed.
   */
  DISTRIBUTION_PLACE("dbp", "Distribution place"),

  /**
   * Use for a person or organization that has exclusive or shared marketing rights for an item.
   */
  DISTRIBUTOR("dst", "Distributor"),

  /**
   * Use for a person or organization who is the donor of a book, manuscript, etc., to its present owner. Donors to previous owners are designated as Former owner [fmo] or Inscriber [ins].
   */
  DONOR("dnr", "Donor"),

  /**
   * Use for a person or organization who prepares artistic or technical drawings.
   */
  DRAFTSMAN("drm", "Draftsman"),

  /**
   * Use for a person or organization to which authorship has been dubiously or incorrectly ascribed.
   */
  DUBIOUS_AUTHOR("dub", "Dubious author"),

  /**
   * Use for a person or organization who prepares for publication a work not primarily his/her own, such as by elucidating text, adding introductory or other critical matter, or technically directing an editorial staff.
   */
  EDITOR("edt", "Editor"),

  /**
   * Use for a person responsible for setting up a lighting rig and focusing the lights for a production, and running the lighting at a performance.
   */
  ELECTRICIAN("elg", "Electrician"),

  /**
   * Use for a person or organization who creates a duplicate printing surface by pressure molding and electrodepositing of metal that is then backed up with lead for printing.
   */
  ELECTROTYPER("elt", "Electrotyper"),

  /**
   * Use for a person or organization that is responsible for technical planning and design, particularly with construction.
   */
  ENGINEER("eng", "Engineer"),

  /**
   * Use for a person or organization who cuts letters, figures, etc. on a surface, such as a wooden or metal plate, for printing.
   */
  ENGRAVER("egr", "Engraver"),

  /**
   * Use for a person or organization who produces text or images for printing by subjecting metal, glass, or some other surface to acid or the corrosive action of some other substance.
   */
  ETCHER("etr", "Etcher"),

  /**
   * Use for the name of the place where an event such as a conference or a concert took place.
   */
  EVENT_PLACE("evp", "Event place"),

  /**
   * Use for a person or organization in charge of the description and appraisal of the value of goods, particularly rare items, works of art, etc.
   */
  EXPERT("exp", "Expert"),

  /**
   * Use for a person or organization that executed the facsimile.
   */
  FACSIMILIST("fac", "Facsimilist"),

  /**
   * Use for a person or organization that manages or supervises the work done to collect raw data or do research in an actual setting or environment (typically applies to the natural and social sciences).
   */
  FIELD_DIRECTOR("fld", "Field director"),

  /**
   * Use for a person or organization who is an editor of a motion picture film. This term is used regardless of the medium upon which the motion picture is produced or manufactured (e.g., acetate film, video tape).
   */
  FILM_EDITOR("flm", "Film editor"),

  /**
   * Use for a person or organization who is identified as the only party or the party of the first part. In the case of transfer of right, this is the assignor, transferor, licensor, grantor, etc. Multiple parties can be named jointly as the first party
   */
  FIRST_PARTY("fpy", "First party"),

  /**
   * Use for a person or organization who makes or imitates something of value or importance, especially with the intent to defraud.
   */
  FORGER("frg", "Forger"),

  /**
   * Use for a person or organization who owned an item at any time in the past. Includes those to whom the material was once presented. A person or organization giving the item to the present owner is designated as Donor [dnr]
   */
  FORMER_OWNER("fmo", "Former owner"),

  /**
   * Use for a person or organization that furnished financial support for the production of the work.
   */
  FUNDER("fnd", "Funder"),

  /**
   * Use for a person responsible for geographic information system (GIS) development and integration with global positioning system data.
   */
  GEOGRAPHIC_INFORMATION_SPECIALIST("gis", "Geographic information specialist"),

  /**
   * Use for a person or organization in memory or honor of whom a book, manuscript, etc. is donated.
   */
  HONOREE("hnr", "Honoree"),

  /**
   * Use for a person who is invited or regularly leads a program (often broadcast) that includes other guests, performers, etc. (e.g., talk show host).
   */
  HOST("hst", "Host"),

  /**
   * Use for a person or organization responsible for the decoration of a work (especially manuscript material) with precious metals or color, usually with elaborate designs and motifs.
   */
  ILLUMINATOR("ilu", "Illuminator"),

  /**
   * Use for a person or organization who conceives, and perhaps also implements, a design or illustration, usually to accompany a written text.
   */
  ILLUSTRATOR("ill", "Illustrator"),

  /**
   * Use for a person who signs a presentation statement.
   */
  INSCRIBER("ins", "Inscriber"),

  /**
   * Use for a person or organization who principally plays an instrument in a musical or dramatic presentation or entertainment.
   */
  INSTRUMENTALIST("itr", "Instrumentalist"),

  /**
   * Use for a person or organization who is interviewed at a consultation or meeting, usually by a reporter, pollster, or some other information gathering agent.
   */
  INTERVIEWEE("ive", "Interviewee"),

  /**
   * Use for a person or organization who acts as a reporter, pollster, or other information gathering agent in a consultation or meeting involving one or more individuals.
   */
  INTERVIEWER("ivr", "Interviewer"),

  /**
   * Use for a person or organization who first produces a particular useful item, or develops a new process for obtaining a known item or result.
   */
  INVENTOR("inv", "Inventor"),

  /**
   * Use for an institution that provides scientific analyses of material samples.
   */
  LABORATORY("lbr", "Laboratory"),

  /**
   * Use for a person or organization that manages or supervises work done in a controlled setting or environment.
   */
  LABORATORY_DIRECTOR("ldr", "Laboratory director"),

  /**
   * Use for a person or organization whose work involves coordinating the arrangement of existing and proposed land features and structures.
   */
  LANDSCAPE_ARCHITECT("lsa", "Landscape architect"),

  /**
   * Use to indicate that a person or organization takes primary responsibility for a particular activity or endeavor. Use with another relator term or code to show the greater importance this person or organization has regarding that particular role. If more than one relator is assigned to a heading, use the Lead relator only if it applies to all the relators.
   */
  LEAD("led", "Lead"),

  /**
   * Use for a person or organization permitting the temporary use of a book, manuscript, etc., such as for photocopying or microfilming.
   */
  LENDER("len", "Lender"),

  /**
   * Use for the party who files a libel in an ecclesiastical or admiralty case.
   */
  LIBELANT("lil", "Libelant"),

  /**
   * Use for a libelant who takes an appeal from one ecclesiastical court or admiralty to another to reverse the judgment.
   */
  LIBELANT_APPELLANT("lit", "Libelant-appellant"),

  /**
   * Use for a libelant against whom an appeal is taken from one ecclesiastical court or admiralty to another to reverse the judgment.
   */
  LIBELANT_APPELLEE("lie", "Libelant-appellee"),

  /**
   * Use for a party against whom a libel has been filed in an ecclesiastical court or admiralty.
   */
  LIBELEE("lel", "Libelee"),

  /**
   * Use for a libelee who takes an appeal from one ecclesiastical court or admiralty to another to reverse the judgment.
   */
  LIBELEE_APPELLANT("let", "Libelee-appellant"),

  /**
   * Use for a libelee against whom an appeal is taken from one ecclesiastical court or admiralty to another to reverse the judgment.
   */
  LIBELEE_APPELLEE("lee", "Libelee-appellee"),

  /**
   * Use for a person or organization who is a writer of the text of an opera, oratorio, etc.
   */
  LIBRETTIST("lbt", "Librettist"),

  /**
   * Use for a person or organization who is an original recipient of the right to print or publish.
   */
  LICENSEE("lse", "Licensee"),

  /**
   * Use for person or organization who is a signer of the license, imprimatur, etc.
   */
  LICENSOR("lso", "Licensor"),

  /**
   * Use for a person or organization who designs the lighting scheme for a theatrical presentation, entertainment, motion picture, etc.
   */
  LIGHTING_DESIGNER("lgd", "Lighting designer"),

  /**
   * Use for a person or organization who prepares the stone or plate for lithographic printing, including a graphic artist creating a design directly on the surface from which printing will be done.
   */
  LITHOGRAPHER("ltg", "Lithographer"),

  /**
   * Use for a person or organization who is a writer of the text of a song.
   */
  LYRICIST("lyr", "Lyricist"),

  /**
   * Use for a person or organization that makes an artifactual work (an object made or modified by one or more persons). Examples of artifactual works include vases, cannons or pieces of furniture.
   */
  MANUFACTURER("mfr", "Manufacturer"),

  /**
   * Use for the named entity responsible for marbling paper, cloth, leather, etc. used in construction of a resource.
   */
  MARBLER("mrb", "Marbler"),

  /**
   * Use for a person or organization performing the coding of SGML, HTML, or XML markup of metadata, text, etc.
   */
  MARKUP_EDITOR("mrk", "Markup editor"),

  /**
   * Use for a person or organization primarily responsible for compiling and maintaining the original description of a metadata set (e.g., geospatial metadata set).
   */
  METADATA_CONTACT("mdc", "Metadata contact"),

  /**
   * Use for a person or organization responsible for decorations, illustrations, letters, etc. cut on a metal surface for printing or decoration.
   */
  METAL_ENGRAVER("mte", "Metal-engraver"),

  /**
   * Use for a person who leads a program (often broadcast) where topics are discussed, usually with participation of experts in fields related to the discussion.
   */
  MODERATOR("mod", "Moderator"),

  /**
   * Use for a person or organization that supervises compliance with the contract and is responsible for the report and controls its distribution. Sometimes referred to as the grantee, or controlling agency.
   */
  MONITOR("mon", "Monitor"),

  /**
   * Use for a person who transcribes or copies musical notation
   */
  MUSIC_COPYIST("mcp", "Music copyist"),

  /**
   * Use for a person responsible for basic music decisions about a production, including coordinating the work of the composer, the sound editor, and sound mixers, selecting musicians, and organizing and/or conducting sound for rehearsals and performances.
   */
  MUSICAL_DIRECTOR("msd", "Musical director"),

  /**
   * Use for a person or organization who performs music or contributes to the musical content of a work when it is not possible or desirable to identify the function more precisely.
   */
  MUSICIAN("mus", "Musician"),

  /**
   * Use for a person who is a speaker relating the particulars of an act, occurrence, or course of events.
   */
  NARRATOR("nrt", "Narrator"),

  /**
   * Use for a person or organization responsible for opposing a thesis or dissertation.
   */
  OPPONENT("opn", "Opponent"),

  /**
   * Use for a person or organization responsible for organizing a meeting for which an item is the report or proceedings.
   */
  ORGANIZER_OF_MEETING("orm", "Organizer of meeting"),

  /**
   * Use for a person or organization performing the work, i.e., the name of a person or organization associated with the intellectual content of the work. This category does not include the publisher or personal affiliation, or sponsor except where it is also the corporate author.
   */
  ORIGINATOR("org", "Originator"),

  /**
   * Use for relator codes from other lists which have no equivalent in the MARC list or for terms which have not been assigned a code.
   */
  OTHER("oth", "Other"),

  /**
   * Use for a person or organization that currently owns an item or collection.
   */
  OWNER("own", "Owner"),

  /**
   * Use for a person or organization responsible for the production of paper, usually from wood, cloth, or other fibrous material.
   */
  PAPERMAKER("ppm", "Papermaker"),

  /**
   * Use for a person or organization that applied for a patent.
   */
  PATENT_APPLICANT("pta", "Patent applicant"),

  /**
   * Use for a person or organization that was granted the patent referred to by the item.
   */
  PATENT_HOLDER("pth", "Patent holder"),

  /**
   * Use for a person or organization responsible for commissioning a work. Usually a patron uses his or her means or influence to support the work of artists, writers, etc. This includes those who commission and pay for individual works.
   */
  PATRON("pat", "Patron"),

  /**
   * Use for a person or organization who exhibits musical or acting skills in a musical or dramatic presentation or entertainment, if specific codes for those functions ([act], [dnc], [itr], [voc], etc.) are not used. If specific codes are used, [prf] is used for a person whose principal skill is not known or specified.
   */
  PERFORMER("prf", "Performer"),

  /**
   * Use for an authority (usually a government agency) that issues permits under which work is accomplished.
   */
  PERMITTING_AGENCY("pma", "Permitting agency"),

  /**
   * Use for a person or organization responsible for taking photographs, whether they are used in their original form or as reproductions.
   */
  PHOTOGRAPHER("pht", "Photographer"),

  /**
   * Use for the party who complains or sues in court in a personal action, usually in a legal proceeding.
   */
  PLAINTIFF("ptf", "Plaintiff"),

  /**
   * Use for a plaintiff who takes an appeal from one court or jurisdiction to another to reverse the judgment, usually in a legal proceeding.
   */
  PLAINTIFF_APPELLANT("ptt", "Plaintiff-appellant"),

  /**
   * Use for a plaintiff against whom an appeal is taken from one court or jurisdiction to another to reverse the judgment, usually in a legal proceeding.
   */
  PLAINTIFF_APPELLEE("pte", "Plaintiff-appellee"),

  /**
   * Use for a person or organization responsible for the production of plates, usually for the production of printed images and/or text.
   */
  PLATEMAKER("plt", "Platemaker"),

  /**
   * Use for a person or organization who prints texts, whether from type or plates.
   */
  PRINTER("prt", "Printer"),

  /**
   * Use for a person or organization who prints illustrations from plates.
   */
  PRINTER_OF_PLATES("pop", "Printer of plates"),

  /**
   * Use for a person or organization who makes a relief, intaglio, or planographic printing surface.
   */
  PRINTMAKER("prm", "Printmaker"),

  /**
   * Use for a person or organization primarily responsible for performing or initiating a process, such as is done with the collection of metadata sets.
   */
  PROCESS_CONTACT("prc", "Process contact"),

  /**
   * Use for a person or organization responsible for the making of a motion picture, including business aspects, management of the productions, and the commercial success of the work.
   */
  PRODUCER("pro", "Producer"),

  /**
   * Use for a person responsible for all technical and business matters in a production.
   */
  PRODUCTION_MANAGER("pmn", "Production manager"),

  /**
   * Use for a person or organization associated with the production (props, lighting, special effects, etc.) of a musical or dramatic presentation or entertainment.
   */
  PRODUCTION_PERSONNEL("prd", "Production personnel"),

  /**
   * Use for a person or organization responsible for the creation and/or maintenance of computer program design documents, source code, and machine-executable digital files and supporting documentation.
   */
  PROGRAMMER("prg", "Programmer"),

  /**
   * Use for a person or organization with primary responsibility for all essential aspects of a project, or that manages a very large project that demands senior level responsibility, or that has overall responsibility for managing projects, or provides overall direction to a project manager.
   */
  PROJECT_DIRECTOR("pdr", "Project director"),

  /**
   * Use for a person who corrects printed matter. For manuscripts, use Corrector [crr].
   */
  PROOFREADER("pfr", "Proofreader"),

  /**
   * Use for the name of the place where a resource is published.
   */
  PUBLICATION_PLACE("pup", "Publication place"),

  /**
   * Use for a person or organization that makes printed matter, often text, but also printed music, artwork, etc. available to the public.
   */
  PUBLISHER("pbl", "Publisher"),

  /**
   * Use for a person or organization who presides over the elaboration of a collective work to ensure its coherence or continuity. This includes editors-in-chief, literary editors, editors of series, etc.
   */
  PUBLISHING_DIRECTOR("pbd", "Publishing director"),

  /**
   * Use for a person or organization who manipulates, controls, or directs puppets or marionettes in a musical or dramatic presentation or entertainment.
   */
  PUPPETEER("ppt", "Puppeteer"),

  /**
   * Use for a person or organization to whom correspondence is addressed.
   */
  RECIPIENT("rcp", "Recipient"),

  /**
   * Use for a person or organization who supervises the technical aspects of a sound or video recording session.
   */
  RECORDING_ENGINEER("rce", "Recording engineer"),

  /**
   * Use for a person or organization who writes or develops the framework for an item without being intellectually responsible for its content.
   */
  REDACTOR("red", "Redactor"),

  /**
   * Use for a person or organization who prepares drawings of architectural designs (i.e., renderings) in accurate, representational perspective to show what the project will look like when completed.
   */
  RENDERER("ren", "Renderer"),

  /**
   * Use for a person or organization who writes or presents reports of news or current events on air or in print.
   */
  REPORTER("rpt", "Reporter"),

  /**
   * Use for an agency that hosts data or material culture objects and provides services to promote long term, consistent and shared use of those data or objects.
   */
  REPOSITORY("rps", "Repository"),

  /**
   * Use for a person who directed or managed a research project.
   */
  RESEARCH_TEAM_HEAD("rth", "Research team head"),

  /**
   * Use for a person who participated in a research project but whose role did not involve direction or management of it.
   */
  RESEARCH_TEAM_MEMBER("rtm", "Research team member"),

  /**
   * Use for a person or organization responsible for performing research.
   */
  RESEARCHER("res", "Researcher"),

  /**
   * Use for the party who makes an answer to the courts pursuant to an application for redress, usually in an equity proceeding.
   */
  RESPONDENT("rsp", "Respondent"),

  /**
   * Use for a respondent who takes an appeal from one court or jurisdiction to another to reverse the judgment, usually in an equity proceeding.
   */
  RESPONDENT_APPELLANT("rst", "Respondent-appellant"),

  /**
   * Use for a respondent against whom an appeal is taken from one court or jurisdiction to another to reverse the judgment, usually in an equity proceeding.
   */
  RESPONDENT_APPELLEE("rse", "Respondent-appellee"),

  /**
   * Use for a person or organization legally responsible for the content of the published material.
   */
  RESPONSIBLE_PARTY("rpy", "Responsible party"),

  /**
   * Use for a person or organization, other than the original choreographer or director, responsible for restaging a choreographic or dramatic work and who contributes minimal new content.
   */
  RESTAGER("rsg", "Restager"),

  /**
   * Use for a person or organization responsible for the review of a book, motion picture, performance, etc.
   */
  REVIEWER("rev", "Reviewer"),

  /**
   * Use for a person or organization responsible for parts of a work, often headings or opening parts of a manuscript, that appear in a distinctive color, usually red.
   */
  RUBRICATOR("rbr", "Rubricator"),

  /**
   * Use for a person or organization who is the author of a motion picture screenplay.
   */
  SCENARIST("sce", "Scenarist"),

  /**
   * Use for a person or organization who brings scientific, pedagogical, or historical competence to the conception and realization on a work, particularly in the case of audio-visual items.
   */
  SCIENTIFIC_ADVISOR("sad", "Scientific advisor"),

  /**
   * Use for a person who is an amanuensis and for a writer of manuscripts proper. For a person who makes pen-facsimiles, use Facsimilist [fac].
   */
  SCRIBE("scr", "Scribe"),

  /**
   * Use for a person or organization who models or carves figures that are three-dimensional representations.
   */
  SCULPTOR("scl", "Sculptor"),

  /**
   * Use for a person or organization who is identified as the party of the second part. In the case of transfer of right, this is the assignee, transferee, licensee, grantee, etc. Multiple parties can be named jointly as the second party.
   */
  SECOND_PARTY("spy", "Second party"),

  /**
   * Use for a person or organization who is a recorder, redactor, or other person responsible for expressing the views of a organization.
   */
  SECRETARY("sec", "Secretary"),

  /**
   * Use for a person or organization who translates the rough sketches of the art director into actual architectural structures for a theatrical presentation, entertainment, motion picture, etc. Set designers draw the detailed guides and specifications for building the set.
   */
  SET_DESIGNER("std", "Set designer"),

  /**
   * Use for a person whose signature appears without a presentation or other statement indicative of provenance. When there is a presentation statement, use Inscriber [ins].
   */
  SIGNER("sgn", "Signer"),

  /**
   * Use for a person or organization who uses his/her/their voice with or without instrumental accompaniment to produce music. A performance may or may not include actual words.
   */
  SINGER("sng", "Singer"),

  /**
   * Use for a person who produces and reproduces the sound score (both live and recorded), the installation of microphones, the setting of sound levels, and the coordination of sources of sound for a production.
   */
  SOUND_DESIGNER("sds", "Sound designer"),

  /**
   * Use for a person who participates in a program (often broadcast) and makes a formalized contribution or presentation generally prepared in advance.
   */
  SPEAKER("spk", "Speaker"),

  /**
   * Use for a person or organization that issued a contract or under the auspices of which a work has been written, printed, published, etc.
   */
  SPONSOR("spn", "Sponsor"),

  /**
   * Use for a person who is in charge of everything that occurs on a performance stage, and who acts as chief of all crews and assistant to a director during rehearsals.
   */
  STAGE_MANAGER("stm", "Stage manager"),

  /**
   * Use for an organization responsible for the development or enforcement of a standard.
   */
  STANDARDS_BODY("stn", "Standards body"),

  /**
   * Use for a person or organization who creates a new plate for printing by molding or copying another printing surface.
   */
  STEREOTYPER("str", "Stereotyper"),

  /**
   * Use for a person relaying a story with creative and/or theatrical interpretation.
   */
  STORYTELLER("stl", "Storyteller"),

  /**
   * Use for a person or organization that supports (by allocating facilities, staff, or other resources) a project, program, meeting, event, data objects, material culture objects, or other entities capable of support.
   */
  SUPPORTING_HOST("sht", "Supporting host"),

  /**
   * Use for a person or organization who does measurements of tracts of land, etc. to determine location, forms, and boundaries.
   */
  SURVEYOR("srv", "Surveyor"),

  /**
   * Use for a person who, in the context of a resource, gives instruction in an intellectual subject or demonstrates while teaching physical skills.
   */
  TEACHER("tch", "Teacher"),

  /**
   * Use for a person who is ultimately in charge of scenery, props, lights and sound for a production.
   */
  TECHNICAL_DIRECTOR("tcd", "Technical director"),

  /**
   * Use for a person under whose supervision a degree candidate develops and presents a thesis, mémoire, or text of a dissertation.
   */
  THESIS_ADVISOR("ths", "Thesis advisor"),

  /**
   * Use for a person who prepares a handwritten or typewritten copy from original material, including from dictated or orally recorded material. For makers of pen-facsimiles, use Facsimilist [fac].
   */
  TRANSCRIBER("trc", "Transcriber"),

  /**
   * Use for a person or organization who renders a text from one language into another, or from an older form of a language into the modern form.
   */
  TRANSLATOR("trl", "Translator"),

  /**
   * Use for a person or organization who designed the type face used in a particular item.
   */
  TYPE_DESIGNER("tyd", "Type designer"),

  /**
   * Use for a person or organization primarily responsible for choice and arrangement of type used in an item. If the typographer is also responsible for other aspects of the graphic design of a book (e.g., Book designer [bkd]), codes for both functions may be needed.
   */
  TYPOGRAPHER("tyg", "Typographer"),

  /**
   * Use for the name of a place where a university that is associated with a resource is located, for example, a university where an academic dissertation or thesis was presented.
   */
  UNIVERSITY_PLACE("uvp", "University place"),

  /**
   * Use for a person or organization in charge of a video production, e.g. the video recording of a stage production as opposed to a commercial motion picture. The videographer may be the camera operator or may supervise one or more camera operators. Do not confuse with cinematographer.
   */
  VIDEOGRAPHER("vdg", "Videographer"),

  /**
   * Use for a person or organization who principally exhibits singing skills in a musical or dramatic presentation or entertainment.
   */
  VOCALIST("voc", "Vocalist"),

  /**
   * Use for a person who verifies the truthfulness of an event or action.
   */
  WITNESS("wit", "Witness"),

  /**
   * Use for a person or organization who makes prints by cutting the image in relief on the end-grain of a wood block.
   */
  WOOD_ENGRAVER("wde", "Wood-engraver"),

  /**
   * Use for a person or organization who makes prints by cutting the image in relief on the plank side of a wood block.
   */
  WOODCUTTER("wdc", "Woodcutter"),

  /**
   * Use for a person or organization who writes significant material which accompanies a sound recording or other audiovisual material.
   */
  WRITER_OF_ACCOMPANYING_MATERIAL("wam", "Writer of accompanying material");

  private final String code;
  private final String name;

  Relator(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public static Relator byCode(String code) {
    for (Relator relator : Relator.values()) {
      if (relator.getCode().equalsIgnoreCase(code)) {
        return relator;
      }
    }
    return null;
  }

}
