package com.whatplayed.dao

import com.whatplayed.api.Artist
import com.whatplayed.api.Placement
import com.whatplayed.api.Song
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.tweak.ResultSetMapper

import java.sql.ResultSet
import java.sql.SQLException

@SuppressWarnings('JdbcResultSetReference')
class PlacementMapper implements ResultSetMapper<Placement> {

    Placement map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Artist artist = new Artist(
                id: r.getLong('artist_id'),
                name: r.getString('artist_name'),
        )
        Song song = new Song(
                id: r.getLong('song_id'),
                title: r.getString('song_title'),
                artist: artist,
        )
        Placement placement = new Placement(
                position: r.getInt('position'),
                song: song,
        )
        return placement
    }

}
