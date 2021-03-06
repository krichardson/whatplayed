package com.whatplayed

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.whatplayed.dao.SourceDAO
import com.whatplayed.modules.SourceModule
import com.whatplayed.resources.SourceResource
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle
import io.dropwizard.Application
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.servlets.tasks.Task
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.migrations.MigrationsBundle
import io.federecio.dropwizard.swagger.SwaggerBundle
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import com.whatplayed.dao.ArtistDAO
import com.whatplayed.dao.ChartDAO
import com.whatplayed.dao.PlacementDAO
import com.whatplayed.dao.PlayDAO
import com.whatplayed.dao.SongDAO
import com.whatplayed.dao.PlaySummaryDAO
import com.whatplayed.modules.ArtistModule
import com.whatplayed.modules.ChartModule
import com.whatplayed.modules.PlayModule
import com.whatplayed.modules.SongModule
import com.whatplayed.resources.ArtistResource
import com.whatplayed.resources.ChartResource
import com.whatplayed.resources.PlayResource
import com.whatplayed.resources.TopPlayResource
import com.whatplayed.tasks.PlaylistTask
import com.whatplayed.dao.jdbi.JodaLocalDateArgumentFactory
import com.whatplayed.dao.jdbi.JodaLocalDateMapper
import com.whatplayed.dao.jdbi.JodaLocalDateTimeArgumentFactory
import com.whatplayed.dao.jdbi.JodaLocalDateTimeMapper
import org.skife.jdbi.v2.DBI

class WhatPlayedApplication extends Application<WhatPlayedConfiguration> {
    static void main(String[] args) throws Exception {
        new WhatPlayedApplication().run(args)
    }

    private final MigrationsBundle<WhatPlayedConfiguration> migrationsBundle =
            new MigrationsBundle<WhatPlayedConfiguration>() {
        @Override
        DataSourceFactory getDataSourceFactory(WhatPlayedConfiguration configuration) {
            return configuration.database
        }
    }

    private final SwaggerBundle<WhatPlayedConfiguration> swaggerBundle = new SwaggerBundle<WhatPlayedConfiguration>() {
        @Override
        protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(WhatPlayedConfiguration configuration) {
            return configuration.swaggerBundleConfiguration
        }
    }

    @Override
    void initialize(Bootstrap<WhatPlayedConfiguration> bootstrap) {
        bootstrap.with {
            addBundle migrationsBundle
            addBundle swaggerBundle
            bootstrap.addBundle new TemplateConfigBundle()
        }
    }

    @Override
    void run(WhatPlayedConfiguration configuration,
             Environment environment) throws ClassNotFoundException {

        environment.objectMapper.registerModule(new JodaModule())
        environment.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        environment.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        DBIFactory factory = new DBIFactory()
        DBI jdbi = factory.build(environment, configuration.database, 'postgresql')
        jdbi.registerArgumentFactory(new JodaLocalDateArgumentFactory())
        jdbi.registerMapper(new JodaLocalDateMapper())
        jdbi.registerArgumentFactory(new JodaLocalDateTimeArgumentFactory())
        jdbi.registerMapper(new JodaLocalDateTimeMapper())

        //DOAs
        SourceDAO sourceDAO = jdbi.onDemand(SourceDAO)
        ArtistDAO artistDAO = jdbi.onDemand(ArtistDAO)
        SongDAO songDAO = jdbi.onDemand(SongDAO)
        ChartDAO chartDAO = jdbi.onDemand(ChartDAO)
        PlacementDAO placementDAO = jdbi.onDemand(PlacementDAO)
        PlayDAO playDAO = jdbi.onDemand(PlayDAO)
        PlaySummaryDAO playSummaryDAO = jdbi.onDemand(PlaySummaryDAO)

        //Modules
        SourceModule sourceModule = new SourceModule(sourceDAO)
        ArtistModule artistModule = new ArtistModule(artistDAO)
        SongModule songModule = new SongModule(songDAO)
        ChartModule chartModule = new ChartModule(chartDAO, placementDAO, artistModule, songModule)
        PlayModule playModule = new PlayModule(
                playDAO, playSummaryDAO, sourceModule, artistModule, songModule)
        //Tasks
        Task playlistTask = new PlaylistTask(playModule)

        environment.jersey().register(new SourceResource(sourceModule))
        environment.jersey().register(new ChartResource(chartModule))
        environment.jersey().register(new ArtistResource(artistModule))
        environment.jersey().register(new PlayResource(playModule, sourceModule))
        environment.jersey().register(new TopPlayResource(playModule, sourceModule))
        environment.admin().addTask(playlistTask)
    }
}
