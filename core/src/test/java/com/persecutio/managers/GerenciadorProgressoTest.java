package com.persecutio.managers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import static org.junit.Assert.*;

public class GerenciadorProgressoTest {

    @Test
    public void testNpcInteractAdvancesPhaseAndFlags() {
        GerenciadorProgresso prog = new GerenciadorProgresso(null);
        assertEquals(0, prog.obterFase());
        prog.onNpcInteract("enfermeira");
        assertTrue(prog.temFlag("falou_enfermeira"));
        assertEquals(1, prog.obterFase());
        String nodo = prog.pegarDialogo();
        assertEquals("enfermeira", nodo);
    }

    @Test
    public void testDocumentFoundSchedulesPending() {
        GerenciadorProgresso prog = new GerenciadorProgresso(null);
        prog.darFlag("porta_destrancada");
        prog.onDocumentFound("documento1", "docid1", false);
        assertTrue(prog.consumirPendente());
        assertEquals("docid1", prog.obterChave());
    }

    @Test
    public void testOnPuzzleSolvedSchedulesDialog() {
        GerenciadorProgresso prog = new GerenciadorProgresso(null);
        prog.onPuzzleSolved();
        assertEquals(3, prog.obterFase());
        String nodo = prog.pegarDialogo();
        assertEquals("porta_clique", nodo);
    }

    @Test
    public void testOnPortaInteractResponses() {
        GerenciadorProgresso prog = new GerenciadorProgresso(null);
        // Simula terminar Primeira e entrar na Missao 2 Umbra
        prog.concluirPrimeira(0f, 0f); // agora missao == 2
        prog.mudarFase(1);
        prog.alternarUmbra(); // torna mundoUmbra == true

        Rectangle area = new Rectangle(0,0,10,10);
        Vector2 spawn = new Vector2(0,0);
        GerenciadorPortas.Porta porta = new GerenciadorPortas.Porta(area, "portaJardim", "label", spawn, null, "video", true, true, true, false, false, "");

        GerenciadorProgresso.PortaResponse resp = prog.onPortaInteract(porta);
        assertNotNull(resp);
        assertEquals(GerenciadorProgresso.PortaResponse.Action.DIALOG, resp.action);
    }
}
