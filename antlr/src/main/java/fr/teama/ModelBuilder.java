package fr.teama;

import fr.teama.exceptions.InvalidTickException;
import fr.teama.grammar.*;
import fr.teama.grammar.MidimlParser;
import fr.teama.structural.*;
import fr.teama.structural.enums.ClassicNoteEnum;
import fr.teama.structural.enums.DrumNoteEnum;
import fr.teama.structural.enums.InstrumentEnum;
import fr.teama.structural.enums.NoteDurationEnum;
import fr.teama.structural.manipulations.AddManipulation;
import fr.teama.structural.manipulations.DeleteManipulation;
import fr.teama.structural.manipulations.NoteDurationManipulation;
import fr.teama.structural.manipulations.NoteNumberManipulation;

import java.util.ArrayList;
import java.util.List;

public class ModelBuilder extends MidimlBaseListener {

    /********************
     ** Business Logic **
     ********************/

    private App theApp = null;
    private boolean built = false;
    private String instrument;




    public App retrieve() {
        if (built) { return theApp; }
        throw new RuntimeException("Cannot retrieve a model that was not created!");
    }

    /*******************
     ** Symbol tables **
     *******************/


    private List<Track> tracks;
    private List<Bar> bars;
    private int currentTempo = 120;
    private int currentResolution = 4;
    private int currentUnityTimeValue = 4;



    /**************************
     ** Listening mechanisms **
     **************************/

    @Override
    public void enterRoot(MidimlParser.RootContext ctx) {
        built = false;
        theApp = new App();
        tracks = new ArrayList<>();
    }
    @Override
    public void exitRoot(MidimlParser.RootContext ctx) {
        theApp.setTracks(tracks);
        this.built = true;
    }

    @Override
    public void enterTitle(MidimlParser.TitleContext ctx) {
        theApp.setName(ctx.name.getText());
    }

    @Override
    public void enterInitialTempo(MidimlParser.InitialTempoContext ctx) {
        currentTempo = Integer.parseInt(ctx.tempo.getText());
    }

    @Override
    public void enterGlobalRythme(MidimlParser.GlobalRythmeContext ctx) {
        currentResolution = Integer.parseInt(ctx.rythme.getText().split("/")[0]);
        currentUnityTimeValue = Integer.parseInt(ctx.rythme.getText().split("/")[1]);
    }

    @Override
    public void enterInstrument(MidimlParser.InstrumentContext ctx) {
        this.instrument = ctx.name.getText();
        Track track = new Track();
        tracks.add(track);
        bars = new ArrayList<>();
        track.setBars(bars);
        switch (instrument) {
            case "BATTERIE":
                track.setInstrument(InstrumentEnum.DRUM);
                break;
            case "PIANO":
                track.setInstrument(InstrumentEnum.PIANO);
                break;
            case "XYLOPHONE":
                track.setInstrument(InstrumentEnum.XYLOPHONE);
                break;
            case "ACCORDEON":
                track.setInstrument(InstrumentEnum.ACCORDION);
                break;
            case "HARMONICA":
                track.setInstrument(InstrumentEnum.HARMONICA);
                break;
            case "GUITARE":
                track.setInstrument(InstrumentEnum.GUITAR);
                break;
            case "GUITARE ELECTRIQUE DISTORSION":
                track.setInstrument(InstrumentEnum.ELECTRIC_GUITAR_OVERDRIVE);
                break;
            case "CONTREBASSE":
                track.setInstrument(InstrumentEnum.BASS);
                break;
            case "GUITARE BASSE MEDIATOR":
                track.setInstrument(InstrumentEnum.ELECTRIC_BASS_PICKED);
                break;
            case "GUITARE BASSE FRETLESS":
                track.setInstrument(InstrumentEnum.ELECTRIC_BASS_FRETLESS);
                break;
            case "VIOLON":
                track.setInstrument(InstrumentEnum.VIOLIN);
                break;
            case "TROMPETTE":
                track.setInstrument(InstrumentEnum.TRUMPET);
                break;
            case "TROMBONE":
                track.setInstrument(InstrumentEnum.TROMBONE);
                break;
            case "SAXOPHONE ALTO":
                track.setInstrument(InstrumentEnum.ALTO_SAX);
                break;
            case "CLARINETTE":
                track.setInstrument(InstrumentEnum.CLARINET);
                break;
            case "FLUTE":
                track.setInstrument(InstrumentEnum.FLUTE);
                break;
            case "WHISTLE":
                track.setInstrument(InstrumentEnum.WHISTLE);
                break;
            case "OCARINA":
                track.setInstrument(InstrumentEnum.OCARINA);
                break;
            case "BANJO":
                track.setInstrument(InstrumentEnum.BANJO);
                break;
            default:
                throw new RuntimeException("Instrument not supported");
        }
    }

    @Override
    public void enterVolume(MidimlParser.VolumeContext ctx) {
        tracks.get(tracks.size() - 1).setVolume(Integer.parseInt(ctx.volumeVal.getText()));
    }

    @Override
    public void enterMute(MidimlParser.MuteContext ctx) {
        tracks.get(tracks.size() - 1).setVolume(0);
    }

    @Override
    public void enterChangeTempo(MidimlParser.ChangeTempoContext ctx) {
        currentTempo = Integer.parseInt(ctx.tempo.getText());
    }

    @Override
    public void enterChangeRythme(MidimlParser.ChangeRythmeContext ctx) {
        currentResolution = Integer.parseInt(ctx.rythme.getText().split("/")[0]);
        currentUnityTimeValue = Integer.parseInt(ctx.rythme.getText().split("/")[1]);
    }

    @Override
    public void enterBar(MidimlParser.BarContext ctx) {
        NormalBar bar = new NormalBar(currentTempo, currentResolution, currentUnityTimeValue);
        if (ctx.name != null) {
            bar.setName(ctx.name.getText());
        }

        MidimlParser.NoteChaineContext noteChaineContext = ctx.noteChaine();

        while (noteChaineContext != null){
            Note note = createNoteFromContext(noteChaineContext.noteSimple());
            bar.addNote(note);
            noteChaineContext = noteChaineContext.noteChaine();
        }

        bars.add(bar);

        if (ctx.REPETITION() != null) {
            int repetition = Integer.parseInt(ctx.REPETITION().getText().substring(1));
            if (repetition < 1) {
                throw new RuntimeException("Repetition must be greater than 0");
            }
            ReusedBar reusedBar = new ReusedBar(repetition-1, bar, new ArrayList<>(), currentTempo, currentResolution, currentUnityTimeValue);
            bars.add(reusedBar);
        }
    }

    private Note createNoteFromContext(MidimlParser.NoteSimpleContext noteSimpleContext) {
        NoteNumber noteNumber;
        int octaveToAdd = 0;
        if (instrument.equals("BATTERIE")) {
            noteNumber = DrumNoteEnum.valueOf(noteSimpleContext.note.getText());
        } else {
            noteNumber = ClassicNoteEnum.valueOf(noteSimpleContext.note.getText());
            if (noteNumber.getNoteNumber() != -1 && noteSimpleContext.octave != null) {
                octaveToAdd = (Integer.parseInt(noteSimpleContext.octave.getText()));
            }
        }
        // Default value with NOIRE
        NoteDurationEnum noteDuration;
        if (noteSimpleContext.duree == null) {
            noteDuration = NoteDurationEnum.N;
        } else {
            noteDuration =  NoteDurationEnum.valueOf(noteSimpleContext.duree.getText());
        }

        Note note;
        if (noteSimpleContext.timing !=null){
            String timingChaine = noteSimpleContext.timing.getText();
            // Parse the timing (int or double) to double
            double timing = Double.parseDouble(timingChaine);
            try {
                note = new Note(noteNumber, noteDuration, (int) (timing * 4));
            } catch (InvalidTickException e) {
                throw new RuntimeException(e);
            }
        }
         else {
            note = new Note(noteNumber, noteDuration);
        }

        if (octaveToAdd != 0) {
            note.setOctave(octaveToAdd);
        }

        if (noteSimpleContext.noteName != null) {
            note.setName(noteSimpleContext.noteName.getText());
        }

        return note;
    }

    @Override
    public void enterReusedBar(MidimlParser.ReusedBarContext ctx) {
        List<Bar> barList = bars.stream()
                .filter(b -> b.getName().isPresent())
                .filter(b -> b.getName().get().equals(ctx.barNameToUse.getText())).collect(java.util.stream.Collectors.toList());

        if (barList.isEmpty()) {
            throw new RuntimeException("Bar " + ctx.barNameToUse.getText()   + " not found");
        }

        Bar bar = barList.get(barList.size() - 1);

        int repetition = 1;
        if (ctx.REPETITION() != null) {
            repetition = Integer.parseInt(ctx.REPETITION().getText().substring(1));
            if (repetition < 1) {
                throw new RuntimeException("Repetition must be greater than 0");
            }
        }
        List<Manipulation> manipulations = new ArrayList<>();
        ReusedBar reusedBar = new ReusedBar(repetition, bar, manipulations, currentTempo, currentResolution, currentUnityTimeValue);

        MidimlParser.ManipulationContext manipulationContext = ctx.manipulation();
        while (manipulationContext != null) {
            if (manipulationContext.ajout() != null) {
                Note note = createNoteFromContext(manipulationContext.ajout().noteSimple());
                if (!note.getTick().isPresent()) {
                    throw new RuntimeException("Note without timing");
                }

                try {
                    if (note.getName().isPresent()) {
                        manipulations.add(new AddManipulation(note.getNoteNumber(), note.getNoteDuration(), note.getTick().get(), note.getName().get()));
                    } else {
                        manipulations.add(new AddManipulation(note.getNoteNumber(), note.getNoteDuration(), note.getTick().get()));
                    }
                } catch (InvalidTickException e) {
                    throw new RuntimeException(e);
                }
            } else if (manipulationContext.suppression() != null) {
                manipulations.add(new DeleteManipulation(manipulationContext.suppression().noteName.getText()));
            } else if (manipulationContext.modifDuration() != null) {
                manipulations.add(new NoteDurationManipulation(manipulationContext.modifDuration().noteName.getText(), NoteDurationEnum.valueOf(manipulationContext.modifDuration().duration.getText())));
            } else if (manipulationContext.modifNumber() != null) {
                NoteNumber noteNumber;
                if (instrument.equals("BATTERIE")) {
                    noteNumber = DrumNoteEnum.valueOf(manipulationContext.modifNumber().number.getText());
                } else {
                    noteNumber = ClassicNoteEnum.valueOf(manipulationContext.modifNumber().number.getText());
                }
                manipulations.add(new NoteNumberManipulation(manipulationContext.modifNumber().noteName.getText(), noteNumber));
            }
            manipulationContext = manipulationContext.manipulation();
        }

        if (ctx.name != null) {
            reusedBar.setName(ctx.name.getText());
        }

        bars.add(reusedBar);
    }
}

